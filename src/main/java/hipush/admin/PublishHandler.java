package hipush.admin;

import hipush.core.Helpers;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Errors;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.UserInfo;
import hipush.services.RouteService;
import hipush.services.UserService;
import hipush.uuid.ClientId;
import hipush.uuid.JobId;
import hipush.uuid.MessageId;
import hipush.uuid.TaobaoDeviceId;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

@Root(path = "/publish")
public class PublishHandler extends BaseHandler {

	private final static Logger LOG = LoggerFactory
			.getLogger(PublishHandler.class);

	@Branch(path = "/private", methods = { "POST" })
	public void doPrivate(ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String clientId = form.getString("client_id");
		String jobId = form.getString("job_id");
		int messageType = form.getInteger("msg_type");
		String content = form.getString("content");
		boolean online = form.getBoolean("online");
		String sign = form.getString("sign");
		if (!ClientId.isValid(clientId)) {
			form.raise("device_id illegal");
			return;
		}
		if (!JobId.isValid(jobId)) {
			form.raise("job_id illegal");
			return;
		}
		if (content.isEmpty()) {
			form.raise("content must not be empty");
			return;
		}
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app_key not exists");
			return;
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key=" + appKey,
				"client_id=" + clientId, "content=" + content, "job_id="
						+ jobId, "msg_type=" + messageType, "online=" + online)) {
			form.raise("signature mismatch!");
			return;
		}
		UserInfo client = UserService.getInstance().getClient(clientId);
		if (client == null) {
			this.renderError(ctx, new Errors.NotFoundError("client not found"));
			return;
		}
		if (app.getId() != client.getAppId()) {
			form.raise("app key and client_id not match");
			return;
		}
		ServiceInstance<String> inst = null;
		String serverId = RouteService.getInstance().getRoute(clientId);
		if (serverId == null) {
			LOG.error("clientId have no route information");
			inst = ZkService.getInstance().getRandomRpc();
		} else {
			inst = ZkService.getInstance().getRpc(serverId);
		}
		String url = String.format("http://%s:%s/publish/private",
				inst.getAddress(), inst.getPort());
		Unirest.post(url).field("client_id", clientId)
				.field("msg_type", messageType).field("job_id", jobId)
				.field("content", content).field("online", online)
				.asJsonAsync();
		this.renderOk(ctx);
	}

	@Branch(path = "/private2", methods = { "POST" })
	public void doPrivate2(ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String sign = form.getString("sign");
		String deviceId = form.getString("device_id");
		int messageType = form.getInteger("msg_type");
		String jobId = form.getString("job_id");
		String content = form.getString("content");
		boolean online = form.getBoolean("online");
		if (!TaobaoDeviceId.isValid(deviceId)) {
			form.raise("device_id illegal");
			return;
		}
		if (!JobId.isValid(jobId)) {
			form.raise("job_id illegal");
			return;
		}
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app not exists");
			return;
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key=" + appKey,
				"device_id=" + deviceId, "content=" + content, "job_id="
						+ jobId, "msg_type=" + messageType, "online=" + online)) {
			form.raise("signature mismatch!");
			return;
		}
		if (content.isEmpty()) {
			form.raise("content must not be empty");
			return;
		}
		String clientId = UserService.getInstance().getClientId(deviceId,
				appKey);
		if (clientId == null) {
			form.raise("client_id not found");
			return;
		}
		ServiceInstance<String> inst = null;
		String serverId = RouteService.getInstance().getRoute(clientId);
		if (serverId == null) {
			LOG.error("clientId have no route information");
			inst = ZkService.getInstance().getRandomRpc();
		} else {
			inst = ZkService.getInstance().getRpc(serverId);
		}
		String url = String.format("http://%s:%s/publish/private",
				inst.getAddress(), inst.getPort());
		Unirest.post(url).field("client_id", clientId)
				.field("msg_type", messageType).field("job_id", jobId)
				.field("content", content).field("online", online)
				.asJsonAsync();
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx);
	}

	@Branch(path = "/multi", methods = { "POST" })
	public void doMulti(final ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String msgId = form.getString("msg_id");
		String topic = form.getString("topic");
		boolean online = form.getBoolean("online");
		String sign = form.getString("sign");
		if (!(MessageId.isMulti(msgId) && MessageId.isValid(msgId))) {
			form.raise("message id is not illegal multi type");
			return;
		}
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app not exists");
			return;
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key=" + appKey,
				"msg_id=" + msgId, "online=" + online, "topic=" + topic)) {
			form.raise("signature mismatch!");
			return;
		}
		final List<ServiceInstance<String>> instances = ZkService.getInstance()
				.getRpcList();

		if (instances == null || instances.isEmpty()) {
			LOG.error("rpc services is not ready");
			this.renderError(ctx, new Errors.ServerError(
					"rpc services is not ready"));
			return;
		}

		final AtomicInteger finished = new AtomicInteger(0);
		final BaseHandler parent = this;
		for (int i = 0; i < instances.size(); i++) {
			final ServiceInstance<String> inst = instances.get(i);
			String url = String.format("http://%s:%s/publish/multi",
					inst.getAddress(), inst.getPort());
			Unirest.post(url).field("msg_id", msgId).field("app_key", appKey)
					.field("topic", topic).field("online", online)
					.asJsonAsync(new Callback<JsonNode>() {

						@Override
						public void completed(HttpResponse<JsonNode> arg0) {
							LOG.info(String
									.format("publish multi for serverid=%s ip=%s port=%s",
											inst.getId(), inst.getAddress(),
											inst.getPort()));
							checkFinished();
						}

						public void checkFinished() {
							int total = finished.incrementAndGet();
							if (total >= instances.size()) {
								parent.setKeepAlive(ctx, true);
								parent.renderOk(ctx);
							}
						}

						@Override
						public void failed(UnirestException arg0) {
							LOG.error(
									String.format(
											"publish multi failed for serverid=%s ip=%s port=%s",
											inst.getId(), inst.getAddress(),
											inst.getPort()), arg0);
							checkFinished();
						}

						@Override
						public void cancelled() {

						}
					});
		}
	}
}
