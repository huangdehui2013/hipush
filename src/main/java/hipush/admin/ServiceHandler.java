package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.x.discovery.ServiceInstance;

@Root(path="/service")
public class ServiceHandler extends BaseHandler {

	@Branch(path="/list", methods={"GET"}, isLoginRequired = true)
	public void getServiceList(ChannelHandlerContext ctx, Form form) {
		List<ServiceInstance<String>> comets = ZkService.getInstance().getCometList();
		List<ServiceInstance<String>> webs = ZkService.getInstance().getWebList();
		List<ServiceInstance<String>> admins = ZkService.getInstance().getAdminList();
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("comets", comets);
		context.put("webs", webs);
		context.put("admins", admins);
		renderTemplate(ctx, "services.mus", context);
	}
	
}
