package hipush.services;

import hipush.db.DaoCenter;
import hipush.db.UserModel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.dao.Dao;

public class UserService extends BaseService {

	private final static Logger LOG = LoggerFactory
			.getLogger(UserService.class);

	private final static UserService instance = new UserService();
	private final static String DEVICE_TO_CLIENT_RKEY_PREFIX = "c:%s:%s";
	private final static String USER_SAVE_QUEUE = "user_save_queue";

	public static UserService getInstance() {
		return instance;
	}

	public String getClientId(String deviceId, String appkey) {
		AppInfo app = AppService.getInstance().getApp(appkey);
		ShardedJedis jedis = jedisPool.getResource();
		String rkey = String.format(DEVICE_TO_CLIENT_RKEY_PREFIX, deviceId,
				app.getId());
		try {
			return jedis.get(rkey);
		} finally {
			jedis.close();
		}
	}

	public void saveClientId(String deviceId, String appkey, String clientId) {
		AppInfo app = AppService.getInstance().getApp(appkey);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String dackey = String.format(DEVICE_TO_CLIENT_RKEY_PREFIX,
					deviceId, app.getId());
			jedis.set(dackey, clientId);
			UserInfo pair = new UserInfo(app.getId(), deviceId);
			jedis.set(clientId, JSON.toJSONString(pair));
			jedis.rpush(USER_SAVE_QUEUE, clientId); // 待刷到数据库里
		} finally {
			jedis.close();
		}
	}

	public boolean saveClientToDB(int count) {
		ShardedJedis jedis = jedisPool.getResource();
		List<UserInfo> users = new ArrayList<UserInfo>();
		try {
			while (true) {
				String clientId = jedis.lpop(USER_SAVE_QUEUE);
				if (clientId == null) {
					break;
				}
				String userStr = jedis.get(clientId);
				if (userStr != null) {
					UserInfo user = JSON.parseObject(userStr, UserInfo.class);
					user.setClientId(clientId);
					users.add(user);
				}
				if (users.size() >= count) {
					break;
				}
			}
		} finally {
			jedis.close();
		}
		if (users.size() == 0) {
			return false;
		}
		Dao<UserModel, String> userDao = DaoCenter.getInstance().getUserDao();
		for (UserInfo user : users) {
			try {
				userDao.create(new UserModel(user.getDeviceId(), user
						.getAppId(), user.getClientId(), System
						.currentTimeMillis()));
			} catch (SQLException e) {
				LOG.error(
						String.format("create user client_id=%s error",
								user.getClientId()), e);
			}
		}
		return true;
	}

	public UserInfo getClient(String clientId) {
		ShardedJedis jedis = jedisPool.getResource();
		String jspair = null;
		try {
			jspair = jedis.get(clientId);
		} finally {
			jedis.close();
		}
		if (jspair == null) {
			return null;
		}
		UserInfo user = JSON.parseObject(jspair, UserInfo.class);
		user.setClientId(clientId);
		return user;
	}

	public void saveToken(String token, String clientId) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.set(token, clientId);
		} finally {
			jedis.close();
		}
	}

	public String getClientId(String token) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.get(token);
		} finally {
			jedis.close();
		}
	}

}
