package hipush.uuid;

import hipush.core.LocalObject;
import hipush.core.Annotations.Concurrent;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Strings;

public interface IdGenerator<T> {

	public T nextId();

	public boolean isValid(String id);

	static class PrefixIdGenerator implements IdGenerator<String> {
		private char prefix;
		private int serverId;
		private volatile long lastTimestamp;
		private AtomicInteger sequence = new AtomicInteger();
		private final static int maxSequence = 1000000; // 5 bytes 16based
		private final static long epoch = 1434253712L;

		public PrefixIdGenerator(char prefix, int serverId) {
			this.prefix = prefix;
			this.serverId = serverId;
		}

		@Override
		public boolean isValid(String id) {
			if (id == null || id.length() != 16) {
				return false;
			}
			if (id.charAt(0) != prefix) {
				return false;
			}
			for (int i = 1; i < id.length(); i++) {
				char c = id.charAt(i);
				if (c < '0' || c > 'z') {
					return false;
				}
			}
			return true;
		}

		@Concurrent
		public String nextId() {
			// prefix{1} + serverId{2} + sequence{5-base16} +
			// relative-timestamp{8-base16}
			long timestamp = System.currentTimeMillis() / 1000;
			int seq = sequence.get();
			// hope you could understand
			if (timestamp == lastTimestamp) {
				if (seq >= maxSequence) {
					seq = tillNextMillis(timestamp);
					timestamp = lastTimestamp;
				} else {
					seq = sequence.incrementAndGet();
				}
			} else {
				if(seq != 0) {
					if(sequence.compareAndSet(seq, 0)) {
						seq = 0;
					} else {
						seq = sequence.incrementAndGet();
					}
				} else {
					seq = sequence.incrementAndGet();
				}
				lastTimestamp = timestamp;
			}
			String sid = String.format("%s%s%s%s", prefix, Strings.padStart(
					Integer.toHexString(serverId), 2, '0'), Strings.padStart(
					Integer.toHexString(seq), 5, '0'), Strings.padStart(
					Long.toHexString(timestamp - epoch), 8, '0'));
			return sid;
		}

		private synchronized int tillNextMillis(long ts) {
			if(ts != lastTimestamp) {
				// double check
				return sequence.incrementAndGet();
			}
			while (true) {
				long now = System.currentTimeMillis() / 1000;
				if (now > ts) {
					lastTimestamp = now;
					sequence.set(0);
					return 0;
				}
			}
		}
	}

	static class UUIDGenerator implements IdGenerator<String> {
		private char prefix;

		public UUIDGenerator(char prefix) {
			this.prefix = prefix;
		}

		@Override
		public String nextId() {
			return prefix + UUID.randomUUID().toString().toLowerCase();
		}

		@Override
		public boolean isValid(String id) {
			if (id == null || id.length() != 37) {
				return false;
			}
			if (id.charAt(0) != prefix) {
				return false;
			}
			for (int i = 1; i < id.length(); i++) {
				char c = id.charAt(i);
				if (i == 9 || i == 14 || i == 19 || i == 24) {
					if (c != '-') {
						return false;
					}
					continue;
				}
				if (!Character.isLowerCase(c) && !Character.isDigit(c)) {
					return false;
				}
			}
			return true;
		}
	}

	static class RandomStringGenerator implements IdGenerator<String> {
		private char prefix;
		private int len;
		private boolean lower;
		private final static char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
				.toCharArray();

		public RandomStringGenerator(char prefix, int len, boolean lower) {
			this.prefix = prefix;
			this.len = len;
			this.lower = lower;
		}

		@Override
		public String nextId() {
			char[] cs = new char[len + 1];
			cs[0] = prefix;
			Random r = LocalObject.random.get();
			for (int i = 0; i < len; i++) {
				cs[i + 1] = chars[r.nextInt(chars.length)];
			}
			String result = new String(cs);
			if (lower) {
				result = result.toLowerCase();
			}
			return result;
		}

		@Override
		public boolean isValid(String id) {
			if (id == null || id.length() != len + 1) {
				return false;
			}
			if (id.charAt(0) != prefix) {
				return false;
			}
			for (int i = 1; i < id.length(); i++) {
				char c = id.charAt(i);
				if (!Character.isLetterOrDigit(c)) {
					return false;
				}
			}
			return true;
		}

	}

}
