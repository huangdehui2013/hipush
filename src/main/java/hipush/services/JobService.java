package hipush.services;

import hipush.db.DaoCenter;
import hipush.db.JobModel;
import hipush.db.JobTypeModel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.dao.Dao;

public class JobService extends BaseService {

	private final static Logger LOG = LoggerFactory.getLogger(JobService.class);

	private final static JobService instance = new JobService();
	private final static String JOB_RKEY = "job:%s";
	private final static String JOB_SENT_RKEY = "job:sent:%s";
	private final static String JOB_REAL_SENT_RKEY = "job:rsent:%s";
	private final static String JOB_ARRIVED_RKEY = "job:arrived:%s";
	private final static String JOB_CLICK_RKEY = "job:click:%s";
	private final static String JOB_OFFLINE_RKEY = "job:offline:%s";
	private final static String JOBS_RKEY = "jobs";
	private final static String JOB_TYPES_RKEY = "job_types";

	public static JobService getInstance() {
		return instance;
	}

	public Map<String, String> getJobTypes() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.hgetAll(JOB_TYPES_RKEY);
		} finally {
			jedis.close();
		}
	}
	
	public List<JobTypeModel> getAllJobTypesFromDb() {
		Dao<JobTypeModel, String> jobTypeDao = DaoCenter.getInstance()
				.getJobTypeDao();
		try {
			return jobTypeDao.queryBuilder().orderBy("createTs", true).query();
		} catch (SQLException e) {
			LOG.error("get all job types from db error", e);
			return Collections.emptyList();
		}
	}

	public String getJobType(String key) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.hget(JOB_TYPES_RKEY, key);
		} finally {
			jedis.close();
		}
	}

	public void delJobType(String key) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.hdel(JOB_TYPES_RKEY, key);
		} finally {
			jedis.close();
		}
		Dao<JobTypeModel, String> jobTypeDao = DaoCenter.getInstance()
				.getJobTypeDao();
		try {
			jobTypeDao.deleteById(key);
		} catch (SQLException e) {
			LOG.error(String.format("delete job type key=%s error", key), e);
		}
	}

	public void updateJobType(String key, String name) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.hset(JOB_TYPES_RKEY, key, name);
		} finally {
			jedis.close();
		}
		Dao<JobTypeModel, String> jobTypeDao = DaoCenter.getInstance()
				.getJobTypeDao();
		try {
			jobTypeDao.createOrUpdate(new JobTypeModel(key, name, System
					.currentTimeMillis()));
		} catch (SQLException e) {
			LOG.error(String.format("delete job type key=%s error", key), e);
		}
	}

	public void initJobTypes() {
		this.updateJobType("test", "测试");
	}

	public void saveJobInfo(JobInfo job) {
		String rkey = String.format(JOB_RKEY, job.getId());
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.set(rkey, JSON.toJSONString(job));
			jedis.zadd(JOBS_RKEY, job.getTs(), job.getId());
		} finally {
			jedis.close();
		}
		Dao<JobModel, String> jobDao = DaoCenter.getInstance().getJobDao();
		try {
			jobDao.createOrUpdate(new JobModel(job.getId(), job.getName(), job
					.getType(), job.getTs()));
		} catch (SQLException e) {
			LOG.error(String.format("save job id=%s error", job.getId()), e);
		}
	}

	public JobInfo getJob(String jobId) {
		String rkey = String.format(JOB_RKEY, jobId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String jobStr = jedis.get(rkey);
			if (jobStr == null) {
				return null;
			}
			return JSON.parseObject(jobStr, JobInfo.class);
		} finally {
			jedis.close();
		}
	}

	public int getJobsCount() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.zcard(JOBS_RKEY).intValue();
		} finally {
			jedis.close();
		}
	}

	public List<JobInfo> getJobs(int offset, int limit) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			Set<String> jobIds = jedis.zrevrange(JOBS_RKEY, offset, offset
					+ limit);
			List<JobInfo> jobs = new ArrayList<JobInfo>(jobIds.size());
			for (String jobId : jobIds) {
				String rkey = String.format(JOB_RKEY, jobId);
				String jobStr = jedis.get(rkey);
				if (jobStr == null) {
					continue;
				}
				JobInfo job = JSON.parseObject(jobStr, JobInfo.class);
				String sents = jedis.get(String.format(JOB_SENT_RKEY, jobId));
				String realSents = jedis.get(String.format(JOB_REAL_SENT_RKEY,
						jobId));
				String arriveds = jedis.get(String.format(JOB_ARRIVED_RKEY,
						jobId));
				String offlines = jedis.get(String.format(JOB_OFFLINE_RKEY,
						jobId));
				String clicks = jedis.get(String.format(JOB_CLICK_RKEY, jobId));
				JobStat stat = new JobStat(jobId);
				if (sents != null) {
					stat.setSentCount(Integer.parseInt(sents));
				}
				if (realSents != null) {
					stat.setRealSentCount(Integer.parseInt(realSents));
				}
				if (arriveds != null) {
					stat.setArrivedCount(Integer.parseInt(arriveds));
				}
				if (offlines != null) {
					stat.setOfflineCount(Integer.parseInt(offlines));
				}
				if (clicks != null) {
					stat.setClickCount(Integer.parseInt(clicks));
				}
				job.setJobStat(stat);
				jobs.add(job);
			}
			return jobs;
		} finally {
			jedis.close();
		}
	}

	public void incrJobStat(JobStat stat) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String jobId = stat.getId();
			if (stat.getSentCount() > 0) {
				jedis.incrBy(String.format(JOB_SENT_RKEY, jobId),
						stat.getSentCount());
			}
			if (stat.getRealSentCount() > 0) {
				jedis.incrBy(String.format(JOB_REAL_SENT_RKEY, jobId),
						stat.getRealSentCount());
			}
			if (stat.getArrivedCount() > 0) {
				jedis.incrBy(String.format(JOB_ARRIVED_RKEY, jobId),
						stat.getArrivedCount());
			}
			if (stat.getOfflineCount() > 0) {
				jedis.incrBy(String.format(JOB_OFFLINE_RKEY, jobId),
						stat.getOfflineCount());
			}
			if (stat.getClickCount() > 0) {
				jedis.incrBy(String.format(JOB_CLICK_RKEY, jobId),
						stat.getClickCount());
			}
		} finally {
			jedis.close();
		}
	}

	public void incrJobStats(Map<String, JobStat> jobStats) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (Entry<String, JobStat> entry : jobStats.entrySet()) {
				String jobId = entry.getKey();
				JobStat stat = entry.getValue();
				if (stat.getSentCount() > 0) {
					jedis.incrBy(String.format(JOB_SENT_RKEY, jobId),
							stat.getSentCount());
				}
				if (stat.getRealSentCount() > 0) {
					jedis.incrBy(String.format(JOB_REAL_SENT_RKEY, jobId),
							stat.getRealSentCount());
				}
				if (stat.getArrivedCount() > 0) {
					jedis.incrBy(String.format(JOB_ARRIVED_RKEY, jobId),
							stat.getArrivedCount());
				}
				if (stat.getOfflineCount() > 0) {
					jedis.incrBy(String.format(JOB_OFFLINE_RKEY, jobId),
							stat.getOfflineCount());
				}
				if (stat.getClickCount() > 0) {
					jedis.incrBy(String.format(JOB_CLICK_RKEY, jobId),
							stat.getClickCount());
				}
			}
		} finally {
			jedis.close();
		}
	}

	public void clearOverdueJobs() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			long startTs = System.currentTimeMillis() - 2 * 86400;
			long endTs = System.currentTimeMillis() - 86400;
			Set<String> jobIds = jedis.zrangeByScore(JOBS_RKEY, startTs, endTs);
			for (String jobId : jobIds) {
				String sstat = jedis.get(String.format(JOB_RKEY, jobId));
				if (sstat == null) {
					jedis.zrem(JOBS_RKEY, jobId);
				}
			}
			Dao<JobModel, String> jobDao = DaoCenter.getInstance().getJobDao();
			try {
				jobDao.deleteIds(jobIds);
			} catch (SQLException e) {
				LOG.error(String.format("delete jobs ids=%s error", jobIds), e);
			}
		} finally {
			jedis.close();
		}
	}

}
