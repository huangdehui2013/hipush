package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.ReportService;
import hipush.services.ServerStat;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.x.discovery.ServiceInstance;

@Root(path = "/comet")
public class CometHandler extends BaseHandler {

	@Branch(path = "/stat", methods = { "GET" }, isLoginRequired = true)
	public void showStats(ChannelHandlerContext ctx, Form form) {
		List<ServerStat> servers = new ArrayList<ServerStat>();
		List<ServiceInstance<String>> services = ZkService.getInstance().getCometList();
		int totalUsersCount = 0;
		for(ServiceInstance<String> service: services) {
			String serverId = service.getId();
			ServerStat stat = ReportService.getInstance().getServerStat(serverId);
			if(stat == null) {
				continue;
			}
			servers.add(stat);
			totalUsersCount += stat.getUsers();
		}
		Map<String, Object> context = new HashMap<String, Object>(1);
		context.put("servers", servers);
		context.put("totalUsersCount", totalUsersCount);
		this.renderTemplate(ctx, "comets.mus", context);
	}

}
