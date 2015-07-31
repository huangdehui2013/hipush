package hipush.uuid;

import hipush.admin.AdminServer;
import hipush.uuid.IdGenerator.PrefixIdGenerator;

public class JobId {

	private static IdGenerator<String> gen = new PrefixIdGenerator('j',
			AdminServer.getInstance().getConfig().getServerId());

	public static String nextId() {
		return gen.nextId();
	}

	public static boolean isValid(String jobId) {
		return gen.isValid(jobId);
	}

}
