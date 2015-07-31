package hipush.comet.rpc;

import hipush.http.HttpServerHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class RpcHandler extends HttpServerHandler {

	@Override
	public void registerHandlers() {
		this.addHandler(new PublishHandler());
		this.addHandler(new AppHandler());
		this.addHandler(new MessageHandler());
		this.addHandler(new SystemHandler());
		this.addHandler(new StatHandler());
		this.addHandler(new TopicHandler());
	}

}
