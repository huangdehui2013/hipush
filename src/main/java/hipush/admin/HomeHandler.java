package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import io.netty.channel.ChannelHandlerContext;

@Root(path = "/")
public class HomeHandler extends BaseHandler {

	@Branch(path = "", methods = { "GET" }, isLoginRequired = true)
	public void index(ChannelHandlerContext ctx, Form form) {
		this.renderTemplate(ctx, "index.mus");
	}

}
