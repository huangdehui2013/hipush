package hipush.uuid;

import hipush.uuid.IdGenerator.UUIDGenerator;

public class SessionId {
	private static IdGenerator<String> gen = new UUIDGenerator('s');

	public static String nextId() {
		return gen.nextId();
	}

	public static boolean isValid(String id) {
		return gen.isValid(id);
	}
}
