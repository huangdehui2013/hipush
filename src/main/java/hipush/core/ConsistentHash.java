package hipush.core;

import hipush.uuid.SessionId;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public class ConsistentHash<T> {

	private int replicas;
	private List<String> keys;
	private Map<String, T> nodes;
	private final static Comparator<String> keySorter = new Comparator<String>() {

		@Override
		public int compare(String arg0, String arg1) {
			long delta = hash(arg0) - hash(arg1);
			return delta > 0 ? 1 : delta < 0 ? -1 : 0;
		}

	};
	
	public ConsistentHash() {
		this(1000);
	}

	public ConsistentHash(int replicas) {
		this(replicas, Collections.<String, T> emptyMap());
	}

	public ConsistentHash(int replicas, Map<String, T> nodesMap) {
		this.replicas = replicas;
		this.keys = new ArrayList<String>();
		this.nodes = new HashMap<String, T>();
		for (Entry<String, T> entry : nodesMap.entrySet()) {
			addNode(entry.getKey(), entry.getValue());
		}
	}

	public synchronized void addNode(String name, T node) {
		for (int i = 0; i < replicas; i++) {
			String sname = name + ':' + i;
			nodes.put(sname, node);
			int index = Collections.binarySearch(keys, sname, keySorter);
			if (index < 0) {
				index = -index - 1;
			} else {
				System.out.println(sname + ":" + index);
			}
			keys.add(index, sname);
		}
	}

	public static long hash(String key) {
		MessageDigest digest = LocalObject.md5.get();
		byte[] data = digest.digest(key.getBytes());
		ByteBuffer buf = ByteBuffer.wrap(data);
		return buf.getLong();
	}

	public synchronized void removeNode(String name) {
		for (int i = 0; i < replicas; i++) {
			String sname = name + ':' + i;
			keys.remove(sname);
			nodes.remove(sname);
		}
	}

	public synchronized T getNode(String key) {
		int index = Collections.binarySearch(keys, key, keySorter);
		if (index < 0) {
			index = -index - 1;
			if (index >= keys.size()) {
				index = 0;
			}
		}
		return nodes.get(keys.get(index));
	}

	public static void main(String[] args) {
		Map<String, Integer> nodesMap = new HashMap<String, Integer>(3);
		int num = 100;
		for (int i = 0; i < num; i++) {
			nodesMap.put("node" + i, i);
		}
		ConsistentHash<Integer> ring = new ConsistentHash<Integer>(1000,
				nodesMap);
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100000; i++) {
			keys.add(SessionId.nextId());
		}
		Map<String, Integer> result1 = new HashMap<String, Integer>();
		for (String key : keys) {
			result1.put(key, ring.getNode(key));
		}
		nodesMap = new HashMap<String, Integer>(3);
		num = 90;
		for (int i = 0; i < num; i++) {
			nodesMap.put("node" + i, i);
		}
		ring = new ConsistentHash<Integer>(1000,
				nodesMap);
		Map<String, Integer> result2 = new HashMap<String, Integer>();
		for (String key : keys) {
			result2.put(key, ring.getNode(key));
		}
		MapDifference<String, Integer> diffs = Maps.difference(result1, result2);
		System.out.println(diffs.entriesInCommon().size());
	}

}
