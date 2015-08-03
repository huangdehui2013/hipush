package hipush.admin;

import hipush.core.Helpers;
import hipush.http.HttpServerHandler;
import hipush.http.TemplateEngine;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class AdminHandler extends HttpServerHandler {

	@Override
	public void registerHandlers() {
		this.addHandler(new ManagerHandler());
		this.addHandler(new AppHandler());
		this.addHandler(new PublishHandler());
		this.addHandler(new MessageHandler());
		this.addHandler(new SystemHandler());
		this.addHandler(new UserHandler());
		this.addHandler(new JobHandler());
		this.addHandler(new CometHandler());
		this.addHandler(new ServiceHandler());
		this.addHandler(new TopicHandler());
		this.addHandler(new HistogramHandler());
		this.addHandler(new ClientEnvironHandler());
		this.addHandler(new HomeHandler());
	}

	private TemplateEngine engine;
	private String staticRoot;

	@Override
	public TemplateEngine getEngine() {
		if (engine == null) {
			engine = new TemplateEngine(AdminServer.getInstance().getConfig()
					.getTemplateRoot());
		}
		return engine;
	}

	@Override
	public String getStaticRoot() {
		if (staticRoot == null) {
			staticRoot = Helpers.getFile(AdminServer.getInstance().getConfig()
					.getStaticRoot());
		}
		return staticRoot;
	}

}
