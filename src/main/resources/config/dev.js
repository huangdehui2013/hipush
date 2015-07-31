var namespace = new JavaImporter(Packages.java.util.ArrayList,
		Packages.redis.clients.jedis.JedisPoolConfig,
		Packages.redis.clients.jedis.JedisShardInfo,
		Packages.redis.clients.jedis.ShardedJedisPool,
		Packages.com.j256.ormlite.jdbc.JdbcPooledConnectionSource,
		Packages.org.apache.curator.retry.ExponentialBackoffRetry,
		Packages.org.apache.curator.framework.CuratorFrameworkFactory);
with (namespace) {
	var retryPolicy = new ExponentialBackoffRetry(1000, 3);
	config.curatorClient = CuratorFrameworkFactory.newClient("127.0.0.1:2181",
			retryPolicy);
	var shards = new ArrayList();
	shards.add(new JedisShardInfo("localhost", 6379));
	var poolConfig = new JedisPoolConfig();
	poolConfig.setMaxTotal(2);
	poolConfig.setMaxIdle(2);
	var pool = new ShardedJedisPool(poolConfig, shards);
	config.registerJedisPool("meta", pool);
	config.registerJedisPool("message", pool);
	config.registerJedisPool("route", pool);
	config.registerJedisPool("topic", pool);
	config.registerJedisPool("user", pool);
	var jdbcUri = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8";
	var mysqlSource = new JdbcPooledConnectionSource(jdbcUri);
	config.mysqlSources.registerSource("user", mysqlSource);
	config.mysqlSources.registerSource("job", mysqlSource);
	config.mysqlSources.registerSource("meta", mysqlSource);
}