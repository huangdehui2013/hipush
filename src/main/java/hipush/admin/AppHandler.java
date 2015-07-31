package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Errors;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
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

@Root(path = "/app")
public class AppHandler extends BaseHandler {
	private final static Logger LOG = LoggerFactory.getLogger(AppHandler.class);

	@Branch(path = "/del", methods = { "GET", "POST" }, isLoginRequired = true)
	public void del(final ChannelHandlerContext ctx, Form form) {
		int appId = form.getInteger("app_id");
		AppInfo app = AppService.getInstance().getApp(appId);
		if (app == null) {
			this.redirect(ctx, "/app/list");
			return;
		}
		AppService.getInstance().removeApp(app);
		final List<ServiceInstance<String>> rpcServices = ZkService
				.getInstance().getRpcList();
		if (rpcServices == null || rpcServices.isEmpty()) {
			LOG.error("rpc services is not ready");
			this.renderError(ctx, new Errors.ServerError(
					"rpc services is not ready"));
			return;
		}
		final AtomicInteger finished = new AtomicInteger(0);
		final BaseHandler parent = this;
		for (final ServiceInstance<String> inst : rpcServices) {
			String url = String.format("http://%s:%s/app/reload",
					inst.getAddress(), inst.getPort());
			Unirest.get(url).asJsonAsync(new Callback<JsonNode>() {

				@Override
				public void cancelled() {

				}

				@Override
				public void completed(HttpResponse<JsonNode> arg0) {
					LOG.info(String.format(
							"app reload for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()));
					checkFinished();
				}

				public void checkFinished() {
					int total = finished.incrementAndGet();
					if (total >= rpcServices.size()) {
						parent.redirect(ctx, "/app/list");
					}
				}

				@Override
				public void failed(UnirestException arg0) {
					LOG.error(String.format(
							"app reload failed for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()),
							arg0);
					checkFinished();
				}

			});
		}
	}

	@Branch(path = "/register", methods = { "GET", "POST" }, isLoginRequired = true)
	public void register(final ChannelHandlerContext ctx, Form form) {
		if (form.isGet()) {
			this.renderTemplate(ctx, "register_app.mus");
			return;
		}
		String key = form.getString("key").toLowerCase();
		String secret = form.getString("secret").toLowerCase();
		String name = form.getString("name");
		String pkg = form.getString("pkg");
		AppService.getInstance().addApp(key, secret, pkg, name);

		final List<ServiceInstance<String>> rpcServices = ZkService
				.getInstance().getRpcList();
		if (rpcServices == null || rpcServices.isEmpty()) {
			LOG.error("rpc services is not ready");
			this.renderError(ctx, new Errors.ServerError(
					"rpc services is not ready"));
			return;
		}

		final AtomicInteger finished = new AtomicInteger(0);
		final BaseHandler parent = this;
		for (final ServiceInstance<String> inst : rpcServices) {
			String url = String.format("http://%s:%s/app/reload",
					inst.getAddress(), inst.getPort());
			Unirest.get(url).asJsonAsync(new Callback<JsonNode>() {

				@Override
				public void cancelled() {

				}

				@Override
				public void completed(HttpResponse<JsonNode> arg0) {
					LOG.info(String.format(
							"app reload for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()));
					checkFinished();
				}

				public void checkFinished() {
					int total = finished.incrementAndGet();
					if (total >= rpcServices.size()) {
						parent.redirect(ctx, "/app/list");
					}
				}

				@Override
				public void failed(UnirestException arg0) {
					LOG.error(String.format(
							"app reload failed for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()),
							arg0);
					checkFinished();
				}

			});
		}
	}

	@Branch(path = "/list", methods = { "GET" }, isLoginRequired = true)
	public void index(ChannelHandlerContext ctx, Form form) {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("apps", AppService.getInstance().getAllAppsFromDb());
		this.renderTemplate(ctx, "apps.mus", context);
	}

}
