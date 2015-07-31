package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Errors;
import hipush.http.Form;
import hipush.http.Root;
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

@Root(path = "/system")
public class SystemHandler extends BaseHandler {

	private final static Logger LOG = LoggerFactory
			.getLogger(SystemHandler.class);

	@Branch(path = "/gc", methods = { "GET", "POST" })
	public void gc(final ChannelHandlerContext ctx, Form form) {
		final List<ServiceInstance<String>> instances = ZkService.getInstance()
				.getRpcList();

		if (instances == null || instances.isEmpty()) {
			LOG.error("rpc services is not ready");
			this.renderError(ctx, new Errors.ServerError("rpc services is not ready"));
			return;
		}

		final AtomicInteger finished = new AtomicInteger(0);
		final BaseHandler parent = this;
		for (final ServiceInstance<String> inst : instances) {
			String url = String.format("http://%s:%s/system/gc",
					inst.getAddress(), inst.getPort());
			Unirest.get(url).asJsonAsync(new Callback<JsonNode>() {

				@Override
				public void completed(HttpResponse<JsonNode> arg0) {
					LOG.info(String.format(
							"system gc for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()));
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
					LOG.error(String.format(
							"system gc failed for serverid=%s ip=%s port=%s",
							inst.getId(), inst.getAddress(), inst.getPort()),
							arg0);
					checkFinished();
				}

				@Override
				public void cancelled() {

				}
			});
		}
	}

}
