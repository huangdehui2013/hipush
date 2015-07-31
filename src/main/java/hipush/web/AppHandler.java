package hipush.web;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppService;
import io.netty.channel.ChannelHandlerContext;

@Root(path="/app")
public class AppHandler extends BaseHandler {

	@Branch(path = "/reload", methods = { "GET", "POST" })
	public void register(ChannelHandlerContext ctx, Form form) {
		AppService.getInstance().loadApps();
		renderOk(ctx);
	}
	
}
