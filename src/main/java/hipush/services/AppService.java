package hipush.services;

import hipush.core.Annotations.Concurrent;
import hipush.db.AppModel;
import hipush.db.DaoCenter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.ShardedJedis;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.dao.Dao;

public class AppService extends BaseService {

	private final static Logger LOG = LoggerFactory.getLogger(AppService.class);

	private final static AppService instance = new AppService();

	public static AppService getInstance() {
		return instance;
	}

	private Map<Integer, AppInfo> idApps = new ConcurrentHashMap<Integer, AppInfo>();
	private Map<String, AppInfo> keyApps = new ConcurrentHashMap<String, AppInfo>();
	private final static String rkey = "apps";

	public AppInfo getApp(int appid) {
		return idApps.get(appid);
	}

	public AppInfo getApp(String appkey) {
		return keyApps.get(appkey);
	}

	public void initApp() {
		if (getAppTotal() == 0) {
			addApp("test-appkey", "test-secret", "com.zhangyue.test", "测试应用");
		}
	}

	public List<AppInfo> getApps() {
		List<AppInfo> apps = new ArrayList<AppInfo>(idApps.size());
		apps.addAll(idApps.values());
		return apps;
	}

	public int maxAppId() {
		int maxId = 0;
		for (int appid : idApps.keySet()) {
			if (appid > maxId) {
				maxId = appid;
			}
		}
		return maxId;
	}

	@Concurrent
	public AppInfo addApp(String key, String secret, String pkg, String name) {
		if (keyApps.containsKey(key)) {
			return keyApps.get(key);
		}
		AppInfo app = new AppInfo(maxAppId() + 1, key, secret, pkg, name,
				System.currentTimeMillis());
		idApps.put(app.getId(), app);
		keyApps.put(app.getKey(), app);
		ShardedJedis jedis = jedisPool.getResource();
		try {
			jedis.hset(rkey, app.getKey(), JSON.toJSONString(app));
		} finally {
			jedis.close();
		}
		Dao<AppModel, Integer> appDao = DaoCenter.getInstance().getAppDao();
		try {
			appDao.createOrUpdate(new AppModel(app.getId(), app.getKey(), app
					.getSecret(), app.getPkg(), app.getName(), app.getTs()));
		} catch (SQLException e) {
			LOG.error(String.format("update app id=%s error", app.getId()), e);
		}
		return app;
	}

	public void removeApp(AppInfo app) {
		if (idApps.containsKey(app.getId())) {
			idApps.remove(app.getId());
		}
		if (keyApps.containsKey(app.getKey())) {
			keyApps.remove(app.getKey());
			ShardedJedis jedis = jedisPool.getResource();
			try {
				jedis.hdel(rkey, app.getKey());
			} finally {
				jedis.close();
			}
		}
		Dao<AppModel, Integer> appDao = DaoCenter.getInstance().getAppDao();
		try {
			appDao.deleteById(app.getId());
		} catch (SQLException e) {
			LOG.error(String.format("delete app id=%s error", app.getId()), e);
		}
	}

	public int getAppTotal() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.hlen(rkey).intValue();
		} finally {
			jedis.close();
		}
	}

	public List<AppModel> getAllAppsFromDb() {
		Dao<AppModel, Integer> appDao = DaoCenter.getInstance().getAppDao();
		try {
			return appDao.queryBuilder().orderBy("createTs", true).query();
		} catch (SQLException e) {
			LOG.error("get all apps from db error", e);
			return Collections.emptyList();
		}
	}

	public void loadApps() {
		ShardedJedis jedis = jedisPool.getResource();
		Map<String, String> appjsons = null;
		try {
			appjsons = jedis.hgetAll(rkey);
		} finally {
			jedis.close();
		}
		Map<Integer, AppInfo> tempIdApps = new ConcurrentHashMap<Integer, AppInfo>();
		Map<String, AppInfo> tempKeyApps = new ConcurrentHashMap<String, AppInfo>();
		for (String appjson : appjsons.values()) {
			AppInfo app = JSON.parseObject(appjson, AppInfo.class);
			tempIdApps.put(app.getId(), app);
			tempKeyApps.put(app.getKey(), app);
		}
		this.keyApps = tempKeyApps;
		this.idApps = tempIdApps;
	}

	public boolean checkSecret(String appKey, String appSecret) {
		AppInfo app = this.getApp(appKey);
		if (app == null) {
			return false;
		}
		return app.getSecret().equals(appSecret);
	}

}
