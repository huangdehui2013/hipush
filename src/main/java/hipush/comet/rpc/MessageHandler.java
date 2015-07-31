package hipush.comet.rpc;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.MessageService;
import io.netty.channel.ChannelHandlerContext;

@Root(path = "/message")
public class MessageHandler extends BaseHandler {

	@Branch(path = "/cache", methods = { "GET", "POST" })
	public void refreshMessage(ChannelHandlerContext ctx, Form form) {
		String msgId = form.getString("msg_id");
		MessageService.getInstance().cacheMessage(msgId);
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx);
	}

}
