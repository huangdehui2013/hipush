package hipush.core;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.ShardedJedisPool;

public class JedisPools {

	private final Map<String, ShardedJedisPool> pools = new HashMap<String, ShardedJedisPool>();
	
	public void registerPool(String name, ShardedJedisPool pool) {
		pools.put(name, pool);
	}
	
	public ShardedJedisPool getPool(String name) {
		return pools.get(name);
	}

}
