package hipush.services;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;

public class ReportService extends BaseService {

	private final static ReportService instance = new ReportService();

	public static ReportService getInstance() {
		return instance;
	}

	private final static String ONLINES_RKEY = "onlines_%s";

	public void saveServerStat(String serverId, ServerStat stat) {
		ShardedJedis jedis = jedisPool.getResource();
		String rkey = String.format(ONLINES_RKEY, serverId);
		try {
			jedis.set(rkey, JSON.toJSONString(stat));
		} finally {
			jedis.close();
		}
	}

	public ServerStat getServerStat(String serverId) {
		ShardedJedis jedis = jedisPool.getResource();
		String rkey = String.format(ONLINES_RKEY, serverId);
		try {
			String ss = jedis.get(rkey);
			if (ss == null) {
				return null;
			}
			return JSON.parseObject(ss, ServerStat.class);
		} finally {
			jedis.close();
		}
	}

}
