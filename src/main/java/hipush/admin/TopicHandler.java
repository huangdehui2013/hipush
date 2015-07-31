package hipush.admin;

import hipush.core.Helpers;
import hipush.core.Helpers.Pager;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.RouteService;
import hipush.services.TopicService;
import hipush.uuid.ClientId;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Tuple;

import com.mashape.unirest.http.Unirest;

@Root(path = "/topic")
public class TopicHandler extends BaseHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(TopicHandler.class);

	@Branch(path = "/list", methods = { "GET", "POST" }, isLoginRequired = true)
	public void list(final ChannelHandlerContext ctx, Form form) {
		int appId = form.getInteger("app_id");
		int pageSize = form.getInteger("page_size", 20);
		int currentPage = form.getInteger("page", 0);
		int total = TopicService.getInstance().getTopicsCount(appId);
		Pager pager = new Helpers.Pager(pageSize, total);
		pager.setCurrentPage(currentPage);
		pager.setUrlPattern("/topic/list?page_size=%s&page=%s&app_id=" + appId);
		Set<Tuple> tuples = TopicService.getInstance().getTopicStats(appId,
				pager.offset(), pager.limit());
		Map<String, Object> context = new HashMap<String, Object>(1);
		context.put("pager", pager);
		context.put("topics", tuples);
		this.renderTemplate(ctx, "topics.mus", context);
	}

	@Branch(path = "/subscribe", methods = { "GET", "POST" })
	public void subscribe(final ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		List<String> clientIds = form.getStrings("client_id");
		String sign = form.getString("sign");
		String topic = form.getString("topic");
		if (!Helpers.isServerTopic(topic)) {
			form.raise("illegal server side topic");
			return;
		}
		if (clientIds.size() == 0) {
			form.raise("client_id must be provided");
			return;
		}
		if (clientIds.size() > 100) {
			form.raise("too many client_ids");
			return;
		}
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app_key not exists");
			return;
		}
		
		StringBuffer clientIdStr = new StringBuffer();
		for (int i = 0; i < clientIds.size(); i++) {
			clientIdStr.append("client_id=" + clientIds.get(i));
			if (i < clientIds.size() - 1) {
				clientIdStr.append('&');
			}
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key" + appKey,
				clientIdStr.toString(), "topic=" + topic)) {
			form.raise("signature mismatch!");
			return;
		}
		for (String clientId : clientIds) {
			if (!ClientId.isValid(clientId)) {
				form.raise("illegal client_id");
				return;
			}
		}
		if (ZkService.getInstance().getRpcSize() == 0) {
			form.raise("service is not ready");
			return;
		}
		Map<String, List<String>> clientSplits = new HashMap<String, List<String>>();
		for (String clientId : clientIds) {
			String serverId = RouteService.getInstance().getRoute(clientId);
			if (serverId == null) {
				LOG.error(String.format(
						"client_id=%s has no route, may be client not exists.",
						clientId));
				continue;
			}
			List<String> serverClients = clientSplits.get(serverId);
			if (serverClients == null) {
				serverClients = new ArrayList<String>();
			}
			serverClients.add(clientId);
		}
		for (Entry<String, List<String>> entry : clientSplits.entrySet()) {
			String serverId = entry.getKey();
			List<String> serverClients = entry.getValue();
			ServiceInstance<String> service = ZkService.getInstance().getRpc(
					serverId);
			if (service == null) {
				service = ZkService.getInstance().getRandomRpc();
				if (service == null) {
					// 有较小的可能， 这里不管了吧
					continue;
				}
			}
			String url = String.format("http://%s:%s/topic/subscribe",
					service.getAddress(), service.getPort());
			Unirest.post(url).field("client_id", serverClients)
					.field("topic", topic).asJsonAsync();
		}
		this.renderOk(ctx);
	}

	@Branch(path = "/unsubscribe", methods = { "GET", "POST" })
	public void unsubscribe(final ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String sign = form.getString("sign");
		List<String> clientIds = form.getStrings("client_id");
		String topic = form.getString("topic");
		if (!Helpers.isServerTopic(topic)) {
			form.raise("illegal server side topic");
			return;
		}
		if (clientIds.size() == 0) {
			form.raise("client_id must be provided");
			return;
		}
		if (clientIds.size() > 100) {
			form.raise("too many client_ids");
			return;
		}
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app_key not exists");
			return;
		}
		StringBuffer clientIdStr = new StringBuffer();
		for (int i = 0; i < clientIds.size(); i++) {
			clientIdStr.append("client_id=" + clientIds.get(i));
			if (i < clientIds.size() - 1) {
				clientIdStr.append('&');
			}
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key" + appKey,
				clientIdStr.toString(), "topic=" + topic)) {
			form.raise("signature mismatch!");
			return;
		}
		for (String clientId : clientIds) {
			if (!ClientId.isValid(clientId)) {
				form.raise("illegal client_id");
				return;
			}
		}
		if (ZkService.getInstance().getRpcSize() == 0) {
			form.raise("service is not ready");
			return;
		}
		Map<String, List<String>> clientSplits = new HashMap<String, List<String>>();
		for (String clientId : clientIds) {
			String serverId = RouteService.getInstance().getRoute(clientId);
			if (serverId == null) {
				LOG.error(String.format(
						"client_id=%s has no route, may be client not exists.",
						clientId));
				continue;
			}
			List<String> serverClients = clientSplits.get(serverId);
			if (serverClients == null) {
				serverClients = new ArrayList<String>();
			}
			serverClients.add(clientId);
		}
		for (Entry<String, List<String>> entry : clientSplits.entrySet()) {
			String serverId = entry.getKey();
			List<String> serverClients = entry.getValue();
			ServiceInstance<String> service = ZkService.getInstance().getRpc(
					serverId);
			if (service == null) {
				service = ZkService.getInstance().getRandomRpc();
				if (service == null) {
					// 有较小的可能， 这里不管了吧
					continue;
				}
			}
			String url = String.format("http://%s:%s/topic/ubsubscribe",
					service.getAddress(), service.getPort());
			Unirest.post(url).field("client_id", serverClients)
					.field("topic", topic).asJsonAsync();
		}
		this.renderOk(ctx);
	}

}