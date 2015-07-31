package hipush.comet.rpc;

import hipush.comet.CometServer;
import hipush.comet.MessageProcessor;
import hipush.comet.protocol.Internals.ServerSubscribeCommand;
import hipush.comet.protocol.Internals.ServerUnSubscribeCommand;
import hipush.core.Helpers;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.TopicService;
import hipush.uuid.ClientId;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

@Root(path = "/topic")
public class TopicHandler extends BaseHandler {

	@Branch(path = "/subscribe", methods = { "GET", "POST" })
	public void subscribe(final ChannelHandlerContext ctx, Form form) {
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
		for (String clientId : clientIds) {
			if (!ClientId.isValid(clientId)) {
				form.raise("illegal client_id");
				return;
			}
		}
		MessageProcessor.getInstance().putMessage(
				new ServerSubscribeCommand(clientIds, topic));
		this.renderOk(ctx);
	}

	@Branch(path = "/unsubscribe", methods = { "GET", "POST" })
	public void unsubscribe(final ChannelHandlerContext ctx, Form form) {
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
		for (String clientId : clientIds) {
			if (!ClientId.isValid(clientId)) {
				form.raise("illegal client_id");
				return;
			}
		}
		MessageProcessor.getInstance().putMessage(
				new ServerUnSubscribeCommand(clientIds, topic));
		this.renderOk(ctx);
	}

	@Branch(path = "/collect_stat", methods = { "GET", "POST" })
	public void collectStat(final ChannelHandlerContext ctx, Form form) {
		String token = form.getString("token");
		List<String> topics = form.getStrings("topic");
		int appId = form.getInteger("app_id");
		int serversCount = form.getInteger("servers_count");
		for (String topic : topics) {
			TopicService.getInstance().collectTopicStat(appId, topic, token,
					CometServer.getInstance().getConfig().getServerId(),
					serversCount);
		}
	}

}