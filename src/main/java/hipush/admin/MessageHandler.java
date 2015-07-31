package hipush.admin;

import hipush.core.Helpers;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Errors;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.JobInfo;
import hipush.services.JobService;
import hipush.services.MessageInfo;
import hipush.services.MessageService;
import hipush.services.RouteService;
import hipush.uuid.JobId;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

@Root(path = "/message")
public class MessageHandler extends BaseHandler {
	private final static Logger LOG = LoggerFactory
			.getLogger(MessageHandler.class);

	@Branch(path = "/sample", methods = { "GET", "POST" }, isLoginRequired = true)
	public void sample(final ChannelHandlerContext ctx, final Form form) {
		if (form.isGet()) {
			this.renderTemplate(ctx, "sample.mus");
			return;
		}
		final String topic = form.getString("topic", "");
		final String content = form.getString("content");
		final int messageType = form.getInteger("msg_type");
		final boolean online = form.getBoolean("online", false);
		final String jobId = JobId.nextId();
		final String clientId = form.getString("client_id", null);
		final String appKey = form.getString("app_key", null);
		JobInfo job = new JobInfo(jobId, "人工测试", "test",
				System.currentTimeMillis());
		JobService.getInstance().saveJobInfo(job);
		if (clientId != null && !clientId.isEmpty()) {
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
			this.redirect(ctx, form.getPath());
			return;
		}
		final MessageInfo msg = MessageService.getInstance().saveTopicMessage(
				messageType, jobId, content);
		final List<ServiceInstance<String>> services = ZkService.getInstance()
				.getRpcList();
		final AtomicInteger finished = new AtomicInteger();
		for (final ServiceInstance<String> service : services) {
			String url = String.format("http://%s:%s/message/cache",
					service.getAddress(), service.getPort());
			Unirest.get(url).queryString("msg_id", msg.getId())
					.asJsonAsync(new Callback<JsonNode>() {

						@Override
						public void completed(HttpResponse<JsonNode> arg0) {
							checkFinished();
						}

						public void checkFinished() {
							int total = finished.incrementAndGet();
							if (total >= services.size()) {
								for (ServiceInstance<String> service : ZkService
										.getInstance().getRpcList()) {
									String url = String.format(
											"http://%s:%s/publish/multi",
											service.getAddress(),
											service.getPort());
									Unirest.post(url).field("topic", topic)
											.field("app_key", appKey)
											.field("msg_id", msg.getId())
											.field("content", content)
											.field("online", online)
											.asJsonAsync();
									redirect(ctx, form.getPath());
								}
							}
						}

						@Override
						public void failed(UnirestException arg0) {
							checkFinished();
						}

						@Override
						public void cancelled() {

						}
					});
		}

	}

	@Branch(path = "/save", methods = { "POST" })
	public void saveMessage(final ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String sign = form.getString("sign");
		int messageType = form.getInteger("msg_type");
		String jobId = form.getString("job_id");
		String content = form.getString("content");
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app_key not exists");
			return;
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key=" + appKey,
				"content=" + content, "job_id=" + jobId, "msg_type="
						+ messageType)) {
			form.raise("signature mismatch!");
			return;
		}
		final MessageInfo msg = MessageService.getInstance().saveTopicMessage(
				messageType, jobId, content);
		final List<ServiceInstance<String>> instances = ZkService.getInstance()
				.getRpcList();
		final Map<String, Object> result = new HashMap<String, Object>();
		result.put("message_id", msg.getId());
		if (instances == null || instances.isEmpty()) {
			LOG.error("rpc services is not ready");
			this.renderError(ctx, new Errors.ServerError(
					"rpc services is not ready"));
			return;
		}
		final AtomicInteger finished = new AtomicInteger(0);
		final BaseHandler parent = this;
		for (final ServiceInstance<String> inst : instances) {
			String url = String.format("http://%s:%s/message/cache",
					inst.getAddress(), inst.getPort());
			Unirest.get(url).queryString("msg_id", msg.getId())
					.asJsonAsync(new Callback<JsonNode>() {

						@Override
						public void completed(HttpResponse<JsonNode> arg0) {
							LOG.info(String
									.format("refresh message for serverid=%s ip=%s port=%s",
											inst.getId(), inst.getAddress(),
											inst.getPort()));
							checkFinished();
						}

						public void checkFinished() {
							int total = finished.incrementAndGet();
							if (total >= instances.size()) {
								parent.setKeepAlive(ctx, true);
								parent.renderOk(ctx, result);
							}
						}

						@Override
						public void failed(UnirestException arg0) {
							LOG.error(
									String.format(String
											.format("refresh mesage failed for serverid=%s ip=%s port=%s",
													inst.getId(),
													inst.getAddress(),
													inst.getPort())), arg0);
							checkFinished();
						}

						@Override
						public void cancelled() {

						}
					});
		}
	}
}
