package hipush.services;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;

public class SessionService extends BaseService {
	private final static SessionService instance = new SessionService();
	private final static int EXPIRE = 3600;

	public static SessionService getInstance() {
		return instance;
	}

	public Session openSession(String sessionId) {
		ShardedJedis jedis = this.jedisPool.getResource();
		String sessionStr = null;
		try {
			sessionStr = jedis.get(sessionId);
		} finally {
			jedis.close();
		}
		if (sessionStr == null) {
			return new Session(sessionId);
		}
		Session session = JSON.parseObject(sessionStr, Session.class);
		session.setSessionId(sessionId);
		return session;
	}

	public void saveSession(Session session) {
		if (session.isClean() || session.getSessionId() == null) {
			return;
		}
		if (session.isEmpty()) {
			ShardedJedis jedis = this.jedisPool.getResource();
			try {
				jedis.del(session.getSessionId());
			} finally {
				jedis.close();
			}
			return;
		}
		ShardedJedis jedis = this.jedisPool.getResource();
		try {
			jedis.setex(session.getSessionId(), EXPIRE,
					JSON.toJSONString(session));
		} finally {
			jedis.close();
		}
	}
}
