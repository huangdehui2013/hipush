package hipush.services;

import redis.clients.jedis.ShardedJedisPool;

public class BaseService {
	protected ShardedJedisPool jedisPool;

	public void setJedisPool(ShardedJedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

}
