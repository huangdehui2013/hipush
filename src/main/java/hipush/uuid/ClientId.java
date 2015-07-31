package hipush.uuid;

import hipush.uuid.IdGenerator.PrefixIdGenerator;
import hipush.web.WebServer;

public class ClientId {

	private static IdGenerator<String> gen = new PrefixIdGenerator('c',
			WebServer.getInstance().getConfig().getServerId());

	public static String nextId() {
		return gen.nextId();
	}

	public static boolean isValid(String clientId) {
		return gen.isValid(clientId);
	}

}
