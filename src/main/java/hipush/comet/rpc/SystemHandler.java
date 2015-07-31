package hipush.comet.rpc;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import io.netty.channel.ChannelHandlerContext;

@Root(path="/system")
public class SystemHandler extends BaseHandler {

	@Branch(path="/gc", methods={"GET", "POST"})
	public void gc(ChannelHandlerContext ctx, Form form) {
		System.gc();
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx);
	}
	
}
