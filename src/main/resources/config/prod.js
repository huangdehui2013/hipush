var namespace = new JavaImporter(
		Packages.java.util.ArrayList,
		Packages.redis.clients.jedis.JedisPoolConfig,
		Packages.redis.clients.jedis.JedisShardInfo,
		Packages.redis.clients.jedis.ShardedJedisPool,
		Packages.com.j256.ormlite.jdbc.JdbcPooledConnectionSource,
		Packages.org.apache.curator.retry.ExponentialBackoffRetry,
		Packages.org.apache.curator.framework.CuratorFrameworkFactory);
with(namespace) {
	var retryPolicy = new ExponentialBackoffRetry(1000, 3);
	config.curatorClient = CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);
	var poolConfig = new JedisPoolConfig();
	poolConfig.setMaxTotal(10);
	poolConfig.setMaxIdle(10);
	var metaShards = new ArrayList();
	metaShards.add(new JedisShardInfo("localhost", 6379));
	var metaPool = new ShardedJedisPool(poolConfig, metaShards);
	
	var messageShards = new ArrayList();
	messageShards.add(new JedisShardInfo("localhost", 6378));
	var messagePool = new ShardedJedisPool(poolConfig, messageShards);
	
	var routeShards = new ArrayList();
	routeShards.add(new JedisShardInfo("localhost", 6377));
	var routePool = new ShardedJedisPool(poolConfig, routeShards);
	
	var topicShards = new ArrayList();
	topicShards.add(new JedisShardInfo("localhost", 6376));
	var topicPool = new ShardedJedisPool(poolConfig, topicShards);
	
	var userShards = new ArrayList();
	userShards.add(new JedisShardInfo("localhost", 6375));
	var userPool = new ShardedJedisPool(poolConfig, userShards);
	
	config.registerJedisPool("meta", metaPool);
	config.registerJedisPool("message", messagePool);
	config.registerJedisPool("route", routePool);
	config.registerJedisPool("topic", topicPool);
	config.registerJedisPool("user", userPool);
	
	var jdbcUri = "jdbc:mysql://localhost:3306/hipush?user=root&password=root&useUnicode=true&characterEncoding=UTF-8";
	var mysqlSource = new JdbcPooledConnectionSource(jdbcUri);
	config.mysqlSources.registerSource("user", mysqlSource);
	config.mysqlSources.registerSource("job", mysqlSource);
	config.mysqlSources.registerSource("meta", mysqlSource);
}