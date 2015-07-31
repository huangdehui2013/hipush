package hipush.core;

import org.apache.curator.framework.CuratorFramework;

import redis.clients.jedis.ShardedJedisPool;

import com.j256.ormlite.support.ConnectionSource;

public class Config {

	private boolean debug;
	private String runmode;
	private int serverId;

	private JedisPools jedisPools = new JedisPools();
	private CuratorFramework curatorClient;
	private MysqlSources mysqlSources = new MysqlSources();

	public ShardedJedisPool getJedisPool(String name) {
		return jedisPools.getPool(name);
	}

	public void registerJedisPool(String name, ShardedJedisPool pool) {
		jedisPools.registerPool(name, pool);
	}

	public MysqlSources getMysqlSources() {
		return mysqlSources;
	}
	
	public ConnectionSource getMysqlSource(String name) {
		return mysqlSources.getSource(name);
	}

	public void setMysqlSources(MysqlSources mysqlSources) {
		this.mysqlSources = mysqlSources;
	}

	public CuratorFramework getCuratorClient() {
		return curatorClient;
	}

	public void setCuratorClient(CuratorFramework curatorClient) {
		this.curatorClient = curatorClient;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getRunmode() {
		return runmode;
	}

	public void setRunmode(String runmode) {
		this.runmode = runmode;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

}
