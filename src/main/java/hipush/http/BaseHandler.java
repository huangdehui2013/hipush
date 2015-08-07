package hipush.http;

import hipush.core.ContextUtils;
import hipush.core.TemplateEngine;
import hipush.services.Session;
import hipush.services.SessionService;
import hipush.uuid.SessionId;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class BaseHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(BaseHandler.class);
	private static Charset charset = Charset.forName("utf8");
	private Map<String, Map<String, Method>> subhandlers;
	private Map<String, Map<String, Boolean>> loginRequiredDefs;
	private String path;

	private TemplateEngine engine;
	private String staticRoot;

	private final static AttributeKey<Boolean> KEEPALIVE = AttributeKey
			.valueOf("keepalive");

	public TemplateEngine getEngine() {
		return engine;
	}

	public void setEngine(TemplateEngine engine) {
		this.engine = engine;
	}

	public String getStaticRoot() {
		return staticRoot;
	}

	public void setStaticRoot(String staticRoot) {
		this.staticRoot = staticRoot;
	}

	public void setKeepAlive(ChannelHandlerContext ctx, boolean keepalive) {
		Attribute<Boolean> attr = ctx.attr(KEEPALIVE);
		attr.set(keepalive);
	}

	public boolean isKeepAlive(ChannelHandlerContext ctx) {
		Attribute<Boolean> attr = ctx.attr(KEEPALIVE);
		return attr.get();
	}

	public void dispatch(ChannelHandlerContext ctx, FullHttpRequest msg) {
		setKeepAlive(ctx, false);
		if (!msg.decoderResult().isSuccess()) {
			renderError(ctx, new Errors.BadArgumentError("http decode failure"));
			return;
		}
		QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());
		String subpath = decoder.path().substring(this.path.length())
				.toLowerCase();
		Map<String, Method> methods = this.subhandlers.get(subpath);
		Map<String, Boolean> loginDefs = this.loginRequiredDefs.get(subpath);
		if (methods == null || loginDefs == null) {
			String reason = String.format("Method not found on path %s",
					decoder.path());
			renderError(ctx, new Errors.NotFoundError(reason));
			return;
		}
		Method method = methods.get(msg.method().toString().toLowerCase());
		Boolean isLoginRequired = loginDefs.get(msg.method().toString()
				.toLowerCase());
		if (method == null || isLoginRequired == null) {
			String reason = String.format("Method %s not allowed on path %s",
					msg.method().name(), decoder.path());
			renderError(ctx, new Errors.MethodNotAllowedError(reason));
			return;
		}
		attachSession(ctx);
		if (isLoginRequired.booleanValue() && !isAuthenticated(ctx)) {
			redirect(ctx, "/manager/login");
			return;
		}
		Form form = new Form.Hybrid(msg);
		try {
			invokeMethod(method, ctx, form);
		} catch (Errors.APIError ex) {
			renderError(ctx, ex);
		}
	}

	public void invokeMethod(Method method, ChannelHandlerContext ctx, Form form) {
		try {
			method.invoke(this, ctx, form);
		} catch (IllegalAccessException e) {
			LOG.error(
					String.format("method is not allowed to access %s",
							method.getName()), e);
			throw new Errors.ServerError("method is not allowed to access");
		} catch (IllegalArgumentException e) {
			LOG.error(
					String.format("method arguments mismatch %s",
							method.getName()), e);
			throw new Errors.ServerError("method arguments mismatch");
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof Errors.APIError) {
				throw (Errors.APIError) target;
			} else {
				LOG.error(
						String.format("method running exception %s",
								method.getName()), target);
				e.printStackTrace();
				throw new Errors.ServerError("SERVER UNKNOWN ERROR!");
			}
		}
	}

	public void initialize(String path) {
		this.path = path;
		Method[] methods = this.getClass().getMethods();
		this.subhandlers = new HashMap<String, Map<String, Method>>();
		this.loginRequiredDefs = new HashMap<String, Map<String, Boolean>>();
		for (Method method : methods) {
			Branch branch = method.getAnnotation(Branch.class);
			if (branch != null) {
				method.setAccessible(true);
				String subpath = branch.path().toLowerCase();
				Map<String, Method> ms = this.subhandlers.get(subpath);
				Map<String, Boolean> ls = this.loginRequiredDefs.get(subpath);
				if (ms == null) {
					ms = new HashMap<String, Method>();
					ls = new HashMap<String, Boolean>();
					this.subhandlers.put(subpath, ms);
					this.loginRequiredDefs.put(subpath, ls);
				}
				for (String hm : branch.methods()) {
					ms.put(hm.toLowerCase(), method);
					ls.put(hm.toLowerCase(), branch.isLoginRequired());
				}
			}
		}
	}

	public void renderTemplate(ChannelHandlerContext ctx, int httpCode,
			String template, Object context) {
		if (this.getEngine() == null) {
			LOG.error("no template engine configuration!");
			ctx.close();
		}
		StringBuffer result = this.getEngine()
				.renderTemplate(template, context);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(200),
				Unpooled.copiedBuffer(result, charset));
		this.writeResult(ctx, response);
	}

	public void renderTemplate(ChannelHandlerContext ctx, String template,
			Object context) {
		renderTemplate(ctx, 200, template, context);
	}

	public void renderTemplate(ChannelHandlerContext ctx, String template) {
		renderTemplate(ctx, template, null);
	}

	public void redirect(ChannelHandlerContext ctx, String uri) {
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(302));
		response.headers().set(HttpHeaderNames.LOCATION, uri);
		this.writeResult(ctx, response);
	}

	public void writeResult(ChannelHandlerContext ctx, FullHttpResponse response) {
		List<String> cookies = ServerCookieEncoder.encode(ContextUtils
				.getCookie(ctx).getCookies());
		for (String cookie : cookies) {
			response.headers().set(HttpHeaderNames.SET_COOKIE, cookie);
		}
		if (isKeepAlive(ctx)) {
			HttpHeaderUtil.setContentLength(response, response.content()
					.readableBytes());
			ctx.writeAndFlush(response);
		} else {
			ctx.writeAndFlush(response)
					.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void renderResult(ChannelHandlerContext ctx, int httpCode,
			Object result) {
		String js = JSON.toJSONString(result);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(httpCode),
				Unpooled.copiedBuffer(js, charset));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE,
				"text/javascript;charset=utf-8");
		this.writeResult(ctx, response);
	}

	public void renderError(ChannelHandlerContext ctx, Errors.APIError error) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("error", error.getErrorCode());
		result.put("reason", error.getReason());
		renderResult(ctx, error.getHttpErrorCode(), result);
	}

	public void renderOk(ChannelHandlerContext ctx, Object result) {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("result", result);
		body.put("ok", true);
		renderResult(ctx, 200, body);
	}

	public void renderOk(ChannelHandlerContext ctx) {
		renderOk(ctx, "OK");
	}

	public boolean isAuthenticated(ChannelHandlerContext ctx) {
		Session session = ContextUtils.getSession(ctx);
		if (session.getUsername() != null) {
			return true;
		}
		return false;
	}

	private void attachSession(ChannelHandlerContext ctx) {
		CookieHolder holder = ContextUtils.getCookie(ctx);
		String sessionId = null;
		if (holder != null && holder.getSessionId() != null) {
			sessionId = holder.getSessionId();
		} else {
			sessionId = SessionId.nextId();
		}
		Session session = SessionService.getInstance().openSession(sessionId);
		ContextUtils.setSession(ctx, session);
	}
}
