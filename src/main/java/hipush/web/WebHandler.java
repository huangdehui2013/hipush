package hipush.web;

import hipush.http.HttpServerHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class WebHandler extends HttpServerHandler {

	@Override
	public void registerHandlers() {
		this.addHandler(new ServiceHandler());
		this.addHandler(new UserHandler());
		this.addHandler(new AppHandler());
		this.addHandler(new MessageHandler());
	}

}
