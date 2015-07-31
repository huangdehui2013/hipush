package hipush.comet;

import hipush.core.ContextUtils;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineManager {

	private final static OnlineManager instance = new OnlineManager();

	public static OnlineManager getInstance() {
		return instance;
	}

	private Map<String, ChannelHandlerContext> clients = new ConcurrentHashMap<String, ChannelHandlerContext>();

	public ChannelHandlerContext getClient(String clientId) {
		return clients.get(clientId);
	}

	public Set<String> getAllClientIds() {
		return clients.keySet();
	}

	public int getCount() {
		return clients.size();
	}

	public boolean isFull() {
		return clients.size() > 100000;
	}

	public ChannelHandlerContext removeClient(String clientId) {
		return clients.remove(clientId);
	}

	public void addClient(String clientId, ChannelHandlerContext ctx) {
		clients.put(clientId, ctx);
	}

	public List<ClientInfo> getDirtyClients() {
		List<ClientInfo> clientInfos = new ArrayList<ClientInfo>();
		for (ChannelHandlerContext ctx : clients.values()) {
			ClientInfo client = ContextUtils.getClient(ctx);
			if (client != null && !client.isEmpty()) {
				clientInfos.add(client);
			}
		}
		return clientInfos;
	}

}
