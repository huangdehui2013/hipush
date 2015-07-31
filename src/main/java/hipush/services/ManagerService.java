package hipush.services;

import hipush.core.PasswordHelpers;
import hipush.db.DaoCenter;
import hipush.db.ManagerModel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.dao.Dao;

public class ManagerService extends BaseService {

	private final static Logger LOG = LoggerFactory
			.getLogger(ManagerService.class);

	private final static ManagerService instance = new ManagerService();

	public static ManagerService getInstance() {
		return instance;
	}

	private final static String RKEY = "managers";

	public void initManager() {
		ShardedJedis jedis = jedisPool.getResource();
		long size = 0;
		try {
			size = jedis.hlen(RKEY);
		} finally {
			jedis.close();
		}
		if (size == 0) {
			saveManager(new ManagerInfo("hipush",
					PasswordHelpers.hash("hipush"), "root"));
		}
	}

	public void saveManager(ManagerInfo manager) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.hset(RKEY, manager.getUsername(), JSON.toJSONString(manager));
		} finally {
			jedis.close();
		}
		Dao<ManagerModel, String> managerDao = DaoCenter.getInstance()
				.getManagerDao();
		try {
			managerDao.createOrUpdate(new ManagerModel(manager.getUsername(),
					manager.getPasswordHash(), manager.getDisplayName(), System
							.currentTimeMillis()));
		} catch (SQLException e) {
			LOG.error(
					String.format("save manager user_name=%s error",
							manager.getUsername()), e);
		}
	}

	public boolean checkLogin(String username, String passwordHash) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String managerStr = jedis.hget(RKEY, username);
			if (managerStr == null) {
				return false;
			}
			ManagerInfo manager = JSON.parseObject(managerStr,
					ManagerInfo.class);
			if (manager.getPasswordHash().equals(passwordHash)) {
				return true;
			}
		} finally {
			jedis.close();
		}
		return false;
	}

	public ManagerInfo getManager(String username) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			String managerStr = jedis.hget(RKEY, username);
			if (managerStr == null) {
				return null;
			}
			return JSON.parseObject(managerStr, ManagerInfo.class);
		} finally {
			jedis.close();
		}
	}

	public void removeManager(String username) {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.hdel(RKEY, username);
		} finally {
			jedis.close();
		}
		Dao<ManagerModel, String> managerDao = DaoCenter.getInstance()
				.getManagerDao();
		try {
			managerDao.deleteById(username);
		} catch (SQLException e) {
			LOG.error(String.format("delete manager user_name=%s error",
					username), e);
		}
	}

	public List<ManagerInfo> getManagers() {
		ShardedJedis jedis = jedisPool.getResource();
		List<ManagerInfo> managers = new ArrayList<ManagerInfo>();
		try {
			Map<String, String> managersStr = jedis.hgetAll(RKEY);
			for (String managerStr : managersStr.values()) {
				managers.add(JSON.parseObject(managerStr, ManagerInfo.class));
			}
		} finally {
			jedis.close();
		}
		return managers;
	}

}
