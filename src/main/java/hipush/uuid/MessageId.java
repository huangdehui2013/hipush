package hipush.uuid;

import hipush.admin.AdminServer;
import hipush.uuid.IdGenerator.PrefixIdGenerator;

public class MessageId {
	private final static char PREFIX_PRIVATE = 'p';
	private final static char PREFIX_MULTI = 'm';
	private static IdGenerator<String> pgen = new PrefixIdGenerator(
			PREFIX_PRIVATE, AdminServer.getInstance().getConfig().getServerId());
	private static IdGenerator<String> mgen = new PrefixIdGenerator(
			PREFIX_MULTI, AdminServer.getInstance().getConfig().getServerId());

	/**
	 * 个性化推荐使用的消息ID，此类消息不做内存缓存
	 * */
	public static String nextPrivateId() {
		return pgen.nextId();
	}

	/**
	 * 主题推送类使用的消息ID，此类消息需要做内存缓存
	 * */
	public static String nextMultiId() {
		return mgen.nextId();
	}

	public static boolean isPrivate(String id) {
		return id.charAt(0) == PREFIX_PRIVATE;
	}

	public static boolean isMulti(String id) {
		return id.charAt(0) == PREFIX_MULTI;
	}

	public static boolean isValid(String id) {
		if (id == null || id.isEmpty()) {
			return false;
		}
		if (isPrivate(id)) {
			return pgen.isValid(id);
		}
		if (isMulti(id)) {
			return mgen.isValid(id);
		}
		return false;
	}
}
