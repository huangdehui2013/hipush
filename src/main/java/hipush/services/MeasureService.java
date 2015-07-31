package hipush.services;

import hipush.core.Annotations.Concurrent;
import hipush.core.LocalObject;
import hipush.core.Pair;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

public class MeasureService extends BaseService {

	private final static MeasureService inst = new MeasureService();

	public static MeasureService getInstance() {
		return inst;
	}

	// Pair<Double, Long> pair<avg, count>
	private Map<String, Pair<Double, Long>> mains = new HashMap<String, Pair<Double, Long>>();
	private Map<String, Pair<Double, Long>> ios = new HashMap<String, Pair<Double, Long>>();
	private final static String MAIN_HISTOGRAM_RKEY = "main_hist:%s:%s";
	private final static String MAIN_HISTOGRAM_TOTAL_RKEY = "main_hist_total";
	private final static String IO_HISTOGRAM_RKEY = "io_hist:%s:%s";
	private final static String IO_HISTOGRAM_TOTAL_RKEY = "io_hist_total";

	public Map<String, Pair<Double, Long>> clearMainHistogram() {
		Map<String, Pair<Double, Long>> savings = mains;
		mains = new HashMap<String, Pair<Double, Long>>();
		return savings;
	}

	public void saveMainHistogram(Map<String, Pair<Double, Long>> savings) {
		SimpleDateFormat formatter = LocalObject.dayFormatter.get();
		String day = formatter.format(new Date());
		String min = "" + System.currentTimeMillis() / (1000 * 60); // subkey存分钟
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (Entry<String, Pair<Double, Long>> entry : savings.entrySet()) {
				String histKey = String.format(MAIN_HISTOGRAM_RKEY, day,
						entry.getKey());
				String costStr = jedis.hget(histKey, min);
				long cost = Math.round(entry.getValue().getLeft());
				if (costStr != null) {
					long costAvg = Long.parseLong(costStr);
					jedis.hset(histKey, min, "" + ((cost + costAvg) >> 1));
				} else {
					jedis.hset(histKey, min,
							"" + Math.round(entry.getValue().getLeft()));
				}
				jedis.zadd(MAIN_HISTOGRAM_TOTAL_RKEY, entry.getValue()
						.getRight(), entry.getKey());
			}
		} finally {
			jedis.close();
		}
	}

	public Set<Tuple> getMainKeys() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.zrevrangeWithScores(MAIN_HISTOGRAM_TOTAL_RKEY, 0, -1);
		} finally {
			jedis.close();
		}
	}

	public Map<Long, Long> getMainHistgramByKey(String day, String key) {
		ShardedJedis jedis = jedisPool.getResource();
		Map<String, String> hist = null;
		try {
			String rkey = String.format(MAIN_HISTOGRAM_RKEY, day, key);
			hist = jedis.hgetAll(rkey);
		} finally {
			jedis.close();
		}
		Map<Long, Long> result = new HashMap<Long, Long>();
		for (Entry<String, String> entry : hist.entrySet()) {
			result.put(Long.parseLong(entry.getKey()),
					Long.parseLong(entry.getValue()));
		}
		return result;
	}

	public Set<Tuple> getIOKeys() {
		ShardedJedis jedis = jedisPool.getResource();
		try {
			return jedis.zrevrangeWithScores(IO_HISTOGRAM_TOTAL_RKEY, 0, -1);
		} finally {
			jedis.close();
		}
	}

	public Map<Long, Long> getIOHistgramByKey(String day, String key) {
		ShardedJedis jedis = jedisPool.getResource();
		Map<String, String> hist = null;
		try {
			String rkey = String.format(IO_HISTOGRAM_RKEY, day, key);
			hist = jedis.hgetAll(rkey);
		} finally {
			jedis.close();
		}
		Map<Long, Long> result = new HashMap<Long, Long>();
		for (Entry<String, String> entry : hist.entrySet()) {
			result.put(Long.parseLong(entry.getKey()),
					Long.parseLong(entry.getValue()));
		}
		return result;
	}

	public Map<String, Pair<Double, Long>> clearIOHistogram() {
		Map<String, Pair<Double, Long>> savings = null;
		synchronized (this) {
			savings = ios;
			ios = new HashMap<String, Pair<Double, Long>>();
		}
		return savings;
	}

	public void saveIOHistogram(Map<String, Pair<Double, Long>> savings) {
		SimpleDateFormat formatter = LocalObject.dayFormatter.get();
		String day = formatter.format(new Date());
		String min = "" + System.currentTimeMillis() / 1000 / 60;
		ShardedJedis jedis = jedisPool.getResource();
		try {
			for (Entry<String, Pair<Double, Long>> entry : savings.entrySet()) {
				String histKey = String.format(IO_HISTOGRAM_RKEY, day,
						entry.getKey());
				String costStr = jedis.hget(histKey, min);
				long cost = Math.round(entry.getValue().getLeft());
				if (costStr != null) {
					long costAvg = Long.parseLong(costStr);
					jedis.hset(histKey, min, "" + ((cost + costAvg) >> 1));
				} else {
					jedis.hset(histKey, min,
							"" + Math.round(entry.getValue().getLeft()));
				}
				jedis.zincrby(IO_HISTOGRAM_TOTAL_RKEY, entry.getValue()
						.getRight(), entry.getKey());
			}
		} finally {
			jedis.close();
		}
	}

	public boolean willSample() {
		Random r = LocalObject.random.get();
		if (r.nextFloat() > 1) {
			return false;
		}
		return true;
	}

	public void sampleMain(String name, long nanoTs, long nanoCost) {
		if (!willSample()) {
			return;
		}
		Pair<Double, Long> pair = mains.get(name);
		if (pair == null) {
			pair = new Pair<Double, Long>(Double.valueOf(0), Long.valueOf(0));
			mains.put(name, pair);
		}
		if (pair.getRight() > 0) {
			pair.setLeft((pair.getLeft() + nanoCost / 1000.0 / pair.getRight())
					/ (1 + 1.0 / pair.getRight()));
		} else {
			pair.setLeft(Double.valueOf(nanoCost / 1000.0));
		}
		pair.setRight(pair.getRight() + 1);
	}

	@Concurrent
	public void sampleIO(String name, long nanoTs, long millisCost) {
		if (!willSample()) {
			return;
		}
		synchronized (this) {
			Pair<Double, Long> pair = ios.get(name);
			if (pair == null) {
				pair = new Pair<Double, Long>(Double.valueOf(0),
						Long.valueOf(0));
				ios.put(name, pair);
			}
			if (pair.getRight() > 0) {
				pair.setLeft((pair.getLeft() + millisCost * 1.0
						/ pair.getRight())
						/ (1 + 1.0 / pair.getRight()));
			} else {
				pair.setLeft(Double.valueOf(millisCost));
			}
			pair.setRight(pair.getRight() + 1);
		}
	}

}
