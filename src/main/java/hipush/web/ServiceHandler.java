package hipush.web;

import hipush.core.LocalObject;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Errors;
import hipush.http.Form;
import hipush.http.Root;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Root(path = "/service")
public class ServiceHandler extends BaseHandler {
	private final static Logger LOG = LoggerFactory.getLogger(ServiceHandler.class);
	
	@Branch(path = "/random", methods = { "GET", "POST" })
	public void randomService(ChannelHandlerContext ctx, Form form) {
		List<ServiceInstance<String>> instances = ZkService.getInstance()
				.getCometList();
		if(instances == null || instances.isEmpty()) {
			LOG.error("comet server is not ready");
			this.renderError(ctx, new Errors.NotFoundError("no service is ready"));
			return;
		}
		Random r = LocalObject.random.get();
		int index = r.nextInt(instances.size());
		ServiceInstance<String> instance = instances.get(index);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("ip", instance.getAddress());
		result.put("port", instance.getPort());
		this.renderOk(ctx, result);
	}
	
	@Branch(path = "/hash", methods = { "GET", "POST" })
	public void hashService(ChannelHandlerContext ctx, Form form) {
		int size = ZkService.getInstance()
				.getCometSize();
		if(size == 0) {
			LOG.error("comet server is not ready");
			this.renderError(ctx, new Errors.NotFoundError("no service is ready"));
			return;
		}
		String clientId = form.getString("client_id");
		ServiceInstance<String> service = ZkService.getInstance().getCometByHash(clientId);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("ip", service.getAddress());
		result.put("port", service.getPort());
		this.renderOk(ctx, result);
	}
	
}
