package hipush.core;

import java.util.HashMap;
import java.util.Map;

import com.j256.ormlite.support.ConnectionSource;

public class MysqlSources {

	private Map<String, ConnectionSource> sources = new HashMap<String, ConnectionSource>();

	public void registerSource(String name, ConnectionSource source) {
		this.sources.put(name, source);
	}
	
	public ConnectionSource getSource(String name) {
		return sources.get(name);
	}
}
