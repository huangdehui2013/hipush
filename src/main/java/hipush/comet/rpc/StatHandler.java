package hipush.comet.rpc;

import hipush.comet.OnlineManager;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import io.netty.channel.ChannelHandlerContext;

@Root(path="/stat")
public class StatHandler extends BaseHandler {

	@Branch(path="/online", methods={"GET"})
	public void showOnlines(ChannelHandlerContext ctx, Form form) {
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx, OnlineManager.getInstance().getCount());
	}
	
}
