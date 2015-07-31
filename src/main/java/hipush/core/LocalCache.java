package hipush.core;

import java.util.SortedMap;
import java.util.TreeMap;

public class LocalCache<T> {

	private SortedMap<String, T> items = new TreeMap<String, T>();
	private int capacity;
	
	public LocalCache(int capacity) {
		this.capacity = capacity;
	}
	
	public synchronized void put(String key, T item) {
		items.put(key, item);
		if(items.size() > capacity) {
			String fkey = items.lastKey();
			items.remove(fkey);
		}
	}
	
	public synchronized T get(String key) {
		return items.get(key);
	}
	
}
