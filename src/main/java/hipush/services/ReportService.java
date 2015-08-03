package hipush.services;

import java.util.Date;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;

import hipush.core.ClientEnvironStat;
import hipush.core.LocalObject;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

public class ReportService extends BaseService {

	private final static ReportService instance = new ReportService();

	public static ReportService getInstance() {
		return instance;
	}

	private final static String ONLINES_RKEY = "onlines_%s";
	private final static String ISP_COUNT_RKEY = "isp_%s";
	private final static String NETWORK_COUNT_RKEY = "network_%s";
	private final static String PHONE_COUNT_RKEY = "phone_%s";
	private final static int ENVIRON_EXPIRE = 86400 * 30;

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

	public void saveClientEnviron(ClientEnvironStat environIncrs) {
		ShardedJedis jedis = jedisPool.getResource();
		String day = LocalObject.dayFormatter.get().format(new Date());
		try {
			String ispKey = String.format(ISP_COUNT_RKEY, day);
			for (int i = 0; i < 4; i++) {
				int incr = environIncrs.getIspIncrs()[i];
				jedis.hincrBy(ispKey, "" + i, incr);
			}
			String networkKey = String.format(NETWORK_COUNT_RKEY, day);
			for (int i = 0; i < 4; i++) {
				int incr = environIncrs.getNetworkIncrs()[i];
				jedis.hincrBy(networkKey, "" + i, incr);
			}
			String phoneKey = String.format(PHONE_COUNT_RKEY, day);
			for (Entry<String, Integer> entry : environIncrs.getPhoneIncrs().entrySet()) {
				jedis.zincrby(phoneKey, entry.getValue(), entry.getKey());
			}
			jedis.expire(ispKey, ENVIRON_EXPIRE);
			jedis.expire(networkKey, ENVIRON_EXPIRE);
			jedis.expire(phoneKey, ENVIRON_EXPIRE);
		} finally {
			jedis.close();
		}
	}

	public ClientEnvironStat getClientEnvironStat(String day) {
		ShardedJedis jedis = jedisPool.getResource();
		ClientEnvironStat environ = new ClientEnvironStat();
		try {
			String ispKey = String.format(ISP_COUNT_RKEY, day);
			String networkKey = String.format(NETWORK_COUNT_RKEY, day);
			String phoneKey = String.format(PHONE_COUNT_RKEY, day);
			for (Entry<String, String> entry : jedis.hgetAll(ispKey).entrySet()) {
				environ.setIsp(Integer.parseInt(entry.getKey()), Integer.parseInt(entry.getValue()));
			}
			for (Entry<String, String> entry : jedis.hgetAll(networkKey).entrySet()) {
				environ.setNetwork(Integer.parseInt(entry.getKey()), Integer.parseInt(entry.getValue()));
			}
			for (Tuple tuple : jedis.zrevrangeWithScores(phoneKey, 0, 100)) {
				environ.setPhone(tuple.getElement(), (int) tuple.getScore());
			}
		} finally {
			jedis.close();
		}
		return environ;
	}

}
