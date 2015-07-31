package hipush.services;

import redis.clients.jedis.ShardedJedis;

public class RouteService extends BaseService {

	private final static RouteService instance = new RouteService();
	private final static String ROUTE_RKEY_PREFIX = "r:%s";

	public static RouteService getInstance() {
		return instance;
	}

	public void saveRoute(String clientId, String serverId) {
		String rkey = String.format(ROUTE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.set(rkey, serverId);
		} finally {
			jedis.close();
		}
	}

	public String getRoute(String clientId) {
		String rkey = String.format(ROUTE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.get(rkey);
		} finally {
			jedis.close();
		}
	}
	
}
