package hipush.services;

import hipush.core.Helpers;
import hipush.core.LocalCache;
import hipush.uuid.MessageId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;

public class MessageService extends BaseService {

	private final static Logger LOG = LoggerFactory
			.getLogger(MessageService.class);

	private final static MessageService instance = new MessageService();
	private final static int MESSAGE_ALIVE = 2 * 86400;
	private final static int MESSAGES_PER_CLIENT = 20;
	private final static int MESSAGE_EXPIRE = 2 * 86400;
	private final static String MESSAGES_KEY = "messages";
	private final static String USER_MESSAGE_RKEY_PREFIX = "m:%s";

	public static MessageService getInstance() {
		return instance;
	}

	private LocalCache<MessageInfo> cache = new LocalCache<MessageInfo>(10000);

	public MessageInfo savePrivateMessage(int type, String jobId, String content) {
		String mid = MessageId.nextPrivateId();
		MessageInfo msg = new MessageInfo(type, jobId, mid, content,
				System.currentTimeMillis());
		return savePrivateMessage(msg);
	}

	public MessageInfo savePrivateMessage(MessageInfo message) {
		int age = (int) ((System.currentTimeMillis() - message.getTs()) / 1000);
		if (age < MESSAGE_ALIVE) {
			ShardedJedis jedis = jedisPool.getResource();
			try {
				jedis.setex(message.getId(), MESSAGE_ALIVE - age,
						JSON.toJSONString(message));
			} finally {
				jedis.close();
			}
		}
		return message;
	}

	public MessageInfo saveTopicMessage(int type, String jobId, String content) {
		String mid = MessageId.nextMultiId();
		MessageInfo msg = new MessageInfo(type, jobId, mid, content,
				System.currentTimeMillis());
		return saveTopicMessage(msg);
	}

	public MessageInfo saveTopicMessage(MessageInfo message) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.setex(message.getId(), MESSAGE_ALIVE,
					JSON.toJSONString(message));
			cache.put(message.getId(), message);
			jedis.zadd(MESSAGES_KEY, message.getTs(), message.getId());
			jedis.zremrangeByScore(MESSAGES_KEY, 0, message.getTs()
					- MESSAGE_EXPIRE * 1000);
		} finally {
			jedis.close();
		}
		return message;
	}

	public void removeUserMessages(String clientId) {
		String rkey = String.format(USER_MESSAGE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.del(rkey);
		} finally {
			jedis.close();
		}
	}

	public List<MessageInfo> getUserMessages(String clientId) {
		List<MessageInfo> messages = new ArrayList<MessageInfo>();
		String rkey = String.format(USER_MESSAGE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		String jsmids = null;
		try {
			jsmids = jedis.get(rkey);
		} finally {
			jedis.close();
		}
		if (jsmids != null) {
			String[] mids = jsmids.split("\\|");

			for (String mid : mids) {
				if (MessageId.isPrivate(mid)) {
					MessageInfo msg = getMessage(mid);
					if (msg != null) {
						messages.add(msg);
					}
				} else {
					MessageInfo msg = cache.get(mid);
					if (msg != null) {
						messages.add(msg);
					}
				}
			}
		}
		return messages;
	}

	public void cacheMessage(String id) {
		ShardedJedis jedis = jedisPool.getResource();
		String jsmsg = null;
		try {
			jsmsg = jedis.get(id);
		} finally {
			jedis.close();
		}
		if (jsmsg != null) {
			MessageInfo msg = JSON.parseObject(jsmsg, MessageInfo.class);
			cache.put(msg.getId(), msg);
		} else {
			LOG.error(String.format("load message id=%s not exists", id));
		}
	}

	public void refreshCache() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			Set<String> mids = null;
			jedis.zremrangeByScore(MESSAGES_KEY, 0, System.currentTimeMillis()
					- MESSAGE_ALIVE * 1000);
			mids = jedis.zrange(MESSAGES_KEY, 0, -1);
			LocalCache<MessageInfo> tempCache = new LocalCache<MessageInfo>(
					1000);
			for (String mid : mids) {
				String jsmsg = null;
				jsmsg = jedis.get(mid);
				if (jsmsg != null) {
					MessageInfo msg = JSON
							.parseObject(jsmsg, MessageInfo.class);
					tempCache.put(msg.getId(), msg);
				}
			}
			cache = tempCache;
		} finally {
			jedis.close();
		}
	}

	public MessageInfo getCachedMessage(String id) {
		return cache.get(id);
	}

	public MessageInfo getMessage(String id) {
		ShardedJedis jedis = jedisPool.getResource();
		String jsmsg = null;
		try {
			jsmsg = jedis.get(id);
		} finally {
			jedis.close();
		}
		if (jsmsg == null) {
			return null;
		}
		return JSON.parseObject(jsmsg, MessageInfo.class);
	}

	public void saveUserMessage(String clientId, String messageId) {
		String rkey = String.format(USER_MESSAGE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String jsmids = jedis.get(rkey);
			jsmids = joinMsgId(jsmids, messageId);
			jedis.setex(rkey, MESSAGE_EXPIRE, jsmids);
		} finally {
			jedis.close();
		}
	}

	public void saveUserMessages(String clientId, List<MessageInfo> messages) {
		String rkey = String.format(USER_MESSAGE_RKEY_PREFIX, clientId);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String jsmids = jedis.get(rkey);
			for (MessageInfo message : messages) {
				jsmids = joinMsgId(jsmids, message.getId());
			}
			jedis.setex(rkey, MESSAGE_EXPIRE, jsmids);
		} finally {
			jedis.close();
		}
	}

	private String joinMsgId(String sourceIds, String id) {
		if (sourceIds == null) {
			sourceIds = "";
		} else {
			sourceIds += "|whatever";
		}
		String[] mids = sourceIds.split("\\|");
		mids[mids.length - 1] = id;
		if (mids.length > MESSAGES_PER_CLIENT) {
			sourceIds = Helpers.join("|", mids, mids.length
					- MESSAGES_PER_CLIENT, mids.length);
		} else {
			sourceIds = Helpers.join("|", mids);
		}
		return sourceIds;
	}

}
