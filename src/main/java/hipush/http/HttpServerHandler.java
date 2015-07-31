package hipush.http;

import hipush.core.ContextUtils;
import hipush.core.Helpers;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieDecoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpServerHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {

	private final static Logger LOG = LoggerFactory
			.getLogger(HttpServerHandler.class);

	private Map<String, BaseHandler> handlers;

	public TemplateEngine getEngine() {
		return null;
	}

	public String getStaticRoot() {
		return null;
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx,
			FullHttpRequest msg) throws Exception {
		LOG.info(String.format("message uri=%s method=%s receive", msg.uri(),
				msg.method().name()));
		if (getStaticRoot() != null && Helpers.isStaticFile(msg.uri())
				&& msg.method() == HttpMethod.GET) {
			msg.setUri(getStaticRoot() + msg.uri());
			StaticHandler.sendFile(ctx, msg);
			return;
		}
		String cookieStr = msg.headers().getAndConvert(HttpHeaderNames.COOKIE);
		if (cookieStr == null) {
			cookieStr = "";
		}
		Set<Cookie> cookies = ServerCookieDecoder.decode(cookieStr);
		ContextUtils.setCookie(ctx, new CookieHolder(cookies));
		QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());
		BaseHandler handler = getHandler(decoder.path());
		long start = System.nanoTime();
		handler.dispatch(ctx, msg);
		long end = System.nanoTime();
		LOG.debug(String.format(
				"executing handler path=%s cost %s nano seconds", msg.uri(),
				end - start));
	}

	public BaseHandler getHandler(String path) {
		if (handlers == null) {
			initHandlers();
		}
		for (Map.Entry<String, BaseHandler> entry : handlers.entrySet()) {
			if (path.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public void initHandlers() {
		this.handlers = new LinkedHashMap<String, BaseHandler>();
		this.registerHandlers();
		this.addHandler(new DefaultHandler());
	}

	public abstract void registerHandlers();

	public void addHandler(BaseHandler handler) {
		Root root = handler.getClass().getAnnotation(Root.class);
		if (root != null) {
			this.handlers.put(root.path(), handler);
			handler.initialize(root.path());
			handler.setEngine(getEngine());
			handler.setStaticRoot(getStaticRoot());
		}
	}

}
