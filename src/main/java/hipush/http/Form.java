package hipush.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;

public class Form {

	private String method;
	private String path;
	protected Map<String, List<String>> query_arguments = new HashMap<String, List<String>>(
			0);
	protected Map<String, List<String>> body_arguments = new HashMap<String, List<String>>(
			0);
	protected Map<String, List<String>> hybrid_arguments = new HashMap<String, List<String>>(
			0);

	public Form(FullHttpRequest msg) {
		this.method = msg.method().toString().toLowerCase();
		QueryStringDecoder qdecoder = new QueryStringDecoder(msg.uri());
		this.path = qdecoder.path();
		this.query_arguments.putAll(qdecoder.parameters());
		this.hybrid_arguments.putAll(qdecoder.parameters());
		if (this.method.equals("post") || this.method.equals("put")) {
			QueryStringDecoder bdecoder = new QueryStringDecoder(msg.content()
					.toString(Charsets.UTF_8), false);
			this.body_arguments.putAll(bdecoder.parameters());
			this.hybrid_arguments.putAll(bdecoder.parameters());
		}
	}

	public String getMethod() {
		return this.method;
	}
	
	public boolean isGet() {
		return this.method.equals("get");
	}
	
	public boolean isPost() {
		return this.method.equals("post");
	}

	public String getPath() {
		return this.path;
	}

	public Map<String, List<String>> getArguments() {
		return this.hybrid_arguments;
	}

	public String getString(String name) {
		List<String> result = getStrings(name);
		if (result.isEmpty()) {
			raise(String.format("argument %s not exists", name));
		}
		return result.get(0);
	}

	public String getString(String name, String defaultValue) {
		List<String> result = getStrings(name);
		if (result.isEmpty()) {
			return defaultValue;
		}
		return result.get(0);
	}

	public int getInteger(String name) {
		List<Integer> result = getIntegers(name);
		if (result.isEmpty()) {
			raise(String.format("argument %s not exists", name));
		}
		return result.get(0);
	}

	public int getInteger(String name, int defaultValue) {
		List<Integer> result = getIntegers(name);
		if (result.isEmpty()) {
			return defaultValue;
		}
		return result.get(0);
	}

	public long getLong(String name) {
		List<Long> result = getLongs(name);
		if (result.isEmpty()) {
			raise(String.format("argument %s not exists", name));
		}
		return result.get(0);
	}

	public long getLong(String name, long defaultValue) {
		List<Long> result = getLongs(name);
		if (result.isEmpty()) {
			return defaultValue;
		}
		return result.get(0);
	}

	public List<String> getStrings(String name) {
		List<String> result = this.getArguments().get(name);
		if (result == null) {
			result = new ArrayList<String>();
		}
		return result;
	}

	public List<Integer> getIntegers(String name) {
		List<String> strings = this.getStrings(name);
		List<Integer> ints = new ArrayList<Integer>(1);
		for (String s : strings) {
			try {
				ints.add(Integer.parseInt(s));
			} catch (NumberFormatException ex) {
				raise(String.format("invalid integer format %s=%s", name, s));
			}
		}
		return ints;
	}

	public void raise(String reason) {
		throw new Errors.BadArgumentError(reason);
	}

	public List<Long> getLongs(String name) {
		List<String> strings = this.getStrings(name);
		List<Long> longs = new ArrayList<Long>(1);
		for (String s : strings) {
			try {
				longs.add(Long.parseLong(s));
			} catch (NumberFormatException ex) {
				raise(String.format("invalid long integer format %s=%s", name,
						s));
			}
		}
		return longs;
	}

	public boolean getBoolean(String name) {
		List<Boolean> result = getBooleans(name);
		if (result.isEmpty()) {
			raise(String.format("argument %s not exists", name));
		}
		return result.get(0);
	}

	public boolean getBoolean(String name, boolean defaultValue) {
		List<Boolean> result = getBooleans(name);
		if (result.isEmpty()) {
			return defaultValue;
		}
		return result.get(0);
	}

	public List<Boolean> getBooleans(String name) {
		List<String> strings = this.getStrings(name);
		List<Boolean> bools = new ArrayList<Boolean>(strings.size());
		for (String s : strings) {
			s = s.toLowerCase();
			if (s.equals("true") || s.equals("on")) {
				bools.add(true);
			} else if (s.equals("false")) {
				bools.add(false);
			} else {
				raise(String.format("invalid boolean format %s=%s", name, s));
			}
		}
		return bools;
	}

	public static class Query extends Form {

		public Query(FullHttpRequest msg) {
			super(msg);
		}

		public Map<String, List<String>> getArguments() {
			return this.query_arguments;
		}

	}

	public static class Post extends Form {

		public Post(FullHttpRequest msg) {
			super(msg);
		}

		public Map<String, List<String>> getArguments() {
			return this.body_arguments;
		}

	}

	public static class Hybrid extends Form {

		public Hybrid(FullHttpRequest msg) {
			super(msg);
		}

		public Map<String, List<String>> getArguments() {
			return this.hybrid_arguments;
		}

	}
}
