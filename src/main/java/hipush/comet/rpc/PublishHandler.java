package hipush.comet.rpc;

import hipush.comet.MessageProcessor;
import hipush.comet.protocol.Internals.PublishMultiCommand;
import hipush.comet.protocol.Internals.PublishPrivateCommand;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import io.netty.channel.ChannelHandlerContext;

@Root(path = "/publish")
public class PublishHandler extends BaseHandler {

	@Branch(path = "/private", methods = { "POST" })
	public void doPrivate(ChannelHandlerContext ctx, Form form) {
		int messageType = form.getInteger("msg_type");
		String clientId = form.getString("client_id");
		String jobId = form.getString("job_id", "");
		String content = form.getString("content");
		boolean online = form.getBoolean("online");

		PublishPrivateCommand command = new PublishPrivateCommand(messageType,
				jobId, clientId, content, online);
		MessageProcessor.getInstance().putMessage(command);
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx);
	}

	@Branch(path = "/multi", methods = { "POST" })
	public void doMulti(ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String topic = form.getString("topic");
		String msgId = form.getString("msg_id");
		boolean online = form.getBoolean("online");
		PublishMultiCommand command = new PublishMultiCommand(appKey, topic,
				msgId, online);
		MessageProcessor.getInstance().putMessage(command);
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx);
	}

}
