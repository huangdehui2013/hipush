package hipush.services;

import hipush.core.Helpers;
import hipush.core.ScheduleManager;
import hipush.uuid.TokenId;
import hipush.zk.ZkService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.curator.x.discovery.ServiceInstance;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

import com.mashape.unirest.http.Unirest;

public class TopicService extends BaseService {

	private final static TopicService instance = new TopicService();
	private final static String USER_TOPICS_RKEY_PREFIX = "t:%s";
	private final static String TOPIC_USERS_RKEY_PREFIX = "s:%s:%s:%s";
	private final static String TOPIC_STAT_RKEY = "topic_stat:%s";
	private final static String TOPIC_STAT_TEMP_RKEY = "topic_stat_temp:%s:%s";
	private final static String TOPIC_STAT_TEMP_COUNT_RKEY = "topic_stat_temp_count:%s:%s";
	private final static String TOPIC_COLLECTS_RKEY = "topics:%s";
	private final static String TOPIC_COLLECT_LOCKER = "topic_stat_locker";

	public static TopicService getInstance() {
		return instance;
	}

	public List<String> getClientTopics(String clientId) {
		ShardedJedis jedis = jedisPool.getResource();
		String rkey = String.format(USER_TOPICS_RKEY_PREFIX, clientId);
		String jstopics = null;
		try {
			jstopics = jedis.get(rkey);
		} finally {
			jedis.close();
		}
		if (jstopics == null) {
			return new ArrayList<String>(0);
		}
		String[] topics = jstopics.split("\\|");
		List<String> topicList = new ArrayList<String>();
		for (String topic : topics) {
			topicList.add(topic);
		}
		return topicList;
	}

	public void saveClientTopics(String clientId, List<String> topics) {
		String rkey = String.format(USER_TOPICS_RKEY_PREFIX, clientId);
		String[] ts = new String[topics.size()];
		topics.toArray(ts);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.set(rkey, Helpers.join("|", ts));
		} finally {
			jedis.close();
		}
	}

	public void clearClientTopics(String clientId) {
		String rkey = String.format(USER_TOPICS_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.del(rkey);
		} finally {
			jedis.close();
		}
	}

	public void saveTopicsMeta(int appId, List<String> topics) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (String topic : topics) {
				String rkey = String.format(TOPIC_COLLECTS_RKEY, appId);
				jedis.sadd(rkey, topic);
			}
		} finally {
			jedis.close();
		}
	}

	public int getTopicsCount(int appId) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String rkey = String.format(TOPIC_COLLECTS_RKEY, appId);
			return jedis.scard(rkey).intValue();
		} finally {
			jedis.close();
		}
	}

	public Set<Tuple> getTopicStats(int appId, int offset, int limit) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String rkey = String.format(TOPIC_STAT_RKEY, appId);
			return jedis.zrevrangeWithScores(rkey, offset, offset + limit);
		} finally {
			jedis.close();
		}
	}

	public void beginCollectTopicStat() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			if (jedis.setnx(TOPIC_COLLECT_LOCKER, "ok").intValue() == 0) {
				return;
			}
			jedis.expire(TOPIC_COLLECT_LOCKER, 3600);
		} finally {
			jedis.close();
		}
		for (AppInfo app : AppService.getInstance().getApps()) {
			this.collectTopicStatForApp(app.getId());
		}
	}

	public void collectTopicStatForApp(final int appId) {
		ShardedJedis jedis = jedisPool.getResource();
		Set<String> topics = null;
		try {
			topics = jedis.smembers(String.format(TOPIC_COLLECTS_RKEY, appId));
		} finally {
			jedis.close();
		}
		final Iterator<String> topicsIter = topics.iterator();
		final String token = TokenId.nextId();
		Runnable runner = new Runnable() {

			@Override
			public void run() {
				List<String> topiclist = new ArrayList<String>();
				while (topicsIter.hasNext()) {
					topiclist.add(topicsIter.next());
					if (topiclist.size() >= 1000) {
						break;
					}
				}
				if (topiclist.size() == 0) {
					return;
				}
				List<ServiceInstance<String>> services = ZkService
						.getInstance().getRpcList();
				for (ServiceInstance<String> service : services) {
					String url = String.format(
							"http://%s:%s/topic/collect_stat",
							service.getAddress(), service.getPort());
					Unirest.post(url).field("app_id", appId)
							.field("topic", topiclist)
							.field("servers_count", services.size())
							.field("token", token).asJsonAsync();
				}
				if (topiclist.size() < 1000) {
					return;
				}
				ScheduleManager.getInstance().delay(10, this);
			}

		};
		ScheduleManager.getInstance().delay(runner);
	}

	public void collectTopicStat(int appId, String topic, String token,
			int serverId, int serversCount) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String usersRkey = String.format(TOPIC_USERS_RKEY_PREFIX, appId,
					topic, serverId);
			int usersCount = jedis.zcard(usersRkey).intValue();
			String tempStatRkey = String.format(TOPIC_STAT_TEMP_RKEY, appId,
					token);
			long totalCount = jedis.hincrBy(tempStatRkey, topic, usersCount);
			String tempStatCountRkey = String.format(
					TOPIC_STAT_TEMP_COUNT_RKEY, appId, token);
			int count = jedis.hincrBy(tempStatCountRkey, topic, 1).intValue();
			if (count == serversCount) {
				String statRkey = String.format(TOPIC_STAT_RKEY, appId);
				jedis.zadd(statRkey, totalCount, topic);
				jedis.hdel(tempStatRkey, topic);
				jedis.hdel(tempStatCountRkey, topic);
			}
		} finally {
			jedis.close();
		}
	}

	public void subscribeTopics(int appId, int serverId, String clientId,
			List<String> topics) {
		long now = System.currentTimeMillis();
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (String topic : topics) {
				String rkey = String.format(TOPIC_USERS_RKEY_PREFIX, appId,
						topic, serverId);
				jedis.zadd(rkey, now, clientId);
			}
		} finally {
			jedis.close();
		}
	}

	public void unsubscribeTopics(int appId, int serverId, String clientId,
			List<String> topics) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (String topic : topics) {
				String rkey = String.format(TOPIC_USERS_RKEY_PREFIX, appId,
						topic, serverId);
				jedis.zrem(rkey, clientId);
			}
		} finally {
			jedis.close();
		}
	}

	public Set<String> getClients(int appId, String topic, int serverId) {
		String rkey = String.format(TOPIC_USERS_RKEY_PREFIX, appId, topic,
				serverId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.zrange(rkey, 0, -1);
		} finally {
			jedis.close();
		}
	}

}
