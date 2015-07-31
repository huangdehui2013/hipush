package hipush.core;

import hipush.comet.ClientInfo;
import hipush.comet.protocol.WriteResponse;
import hipush.http.CookieHolder;
import hipush.services.Session;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextUtils {
	
	private final static Logger LOG = LoggerFactory.getLogger(ContextUtils.class);
	
	private final static AttributeKey<ClientInfo> CLIENT_KEY = AttributeKey
			.valueOf("client");

	public static void attachClient(ChannelHandlerContext ctx, ClientInfo client) {
		ctx.attr(CLIENT_KEY).set(client);
	}

	public static ClientInfo getClient(ChannelHandlerContext ctx) {
		return ctx.attr(CLIENT_KEY).get();
	}

	private final static AttributeKey<List<WriteResponse>> PENDINGS_KEY = AttributeKey
			.valueOf("pendings");

	public static void addPendingMessage(ChannelHandlerContext ctx,
			WriteResponse message) {
		Attribute<List<WriteResponse>> pendingsAttr = ctx.attr(PENDINGS_KEY);
		List<WriteResponse> pendings = pendingsAttr.get();
		if (pendings == null) {
			pendings = new Vector<WriteResponse>(2);
			pendingsAttr.set(pendings);
		}
		if(pendings.size() < 5) {
			pendings.add(message);
		} else {
			LOG.error("too much pendings for channel, so discard it!");
		}
	}

	public static List<WriteResponse> clearPendingMessages(
			ChannelHandlerContext ctx) {
		Attribute<List<WriteResponse>> pendingsAttr = ctx.attr(PENDINGS_KEY);
		List<WriteResponse> pendings = pendingsAttr.get();
		pendingsAttr.remove();
		if (pendings == null) {
			pendings = Collections.emptyList();
		}
		return pendings;
	}

	public static void writeAndFlush(final ChannelHandlerContext ctx,
			final WriteResponse message, final ICallback<Future<Void>> callback) {
		if (ctx.channel().isWritable()) {
			ctx.channel().writeAndFlush(message)
					.addListener(new FutureListener<Void>() {

						@Override
						public void operationComplete(Future<Void> future)
								throws Exception {
							if (!future.isSuccess()) {
								addPendingMessage(ctx, message);
							}
							if (callback != null) {
								callback.invoke(future);
							}
						}
					});
		} else {
			addPendingMessage(ctx, message);
		}
	}

	public static void writeAndFlush(final ChannelHandlerContext ctx,
			final WriteResponse message) {
		writeAndFlush(ctx, message, null);
	}
	
	private final static AttributeKey<CookieHolder> COOKIE = AttributeKey.valueOf("cookie");
	
	public static void setCookie(ChannelHandlerContext ctx, CookieHolder cookie) {
		Attribute<CookieHolder> attr = ctx.attr(COOKIE);
		attr.set(cookie);
	}
	
	public static CookieHolder getCookie(ChannelHandlerContext ctx) {
		Attribute<CookieHolder> attr = ctx.attr(COOKIE);
		return attr.get();
	}
	
	private final static AttributeKey<Session> SESSION = AttributeKey.valueOf("session");
	
	public static void setSession(ChannelHandlerContext ctx, Session session) {
		Attribute<Session> attr = ctx.attr(SESSION);
		attr.set(session);
	}
	
	public static Session getSession(ChannelHandlerContext ctx) {
		Attribute<Session> attr = ctx.attr(SESSION);
		return attr.get();
	}

}
