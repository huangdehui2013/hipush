package hipush.admin;

import hipush.core.ContextUtils;
import hipush.core.PasswordHelpers;
import hipush.core.RsaHelpers;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.CookieHolder;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.ManagerInfo;
import hipush.services.ManagerService;
import hipush.services.Session;
import hipush.services.SessionService;
import hipush.zk.ZkService;
import io.netty.channel.ChannelHandlerContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

@Root(path = "/manager")
public class ManagerHandler extends BaseHandler {

	private final static Logger LOG = LoggerFactory
			.getLogger(ManagerHandler.class);

	@Branch(path = "/register", methods = { "GET", "POST" }, isLoginRequired = true)
	public void register(ChannelHandlerContext ctx, Form form) {
		if(form.isGet()) {
			this.renderTemplate(ctx, "register_manager.mus");
			return;
		}
		String username = form.getString("username");
		String password = form.getString("password");
		String passwordHash = PasswordHelpers.hash(password);
		String displayName = form.getString("display_name");
		ManagerInfo manager = new ManagerInfo(username, passwordHash,
				displayName);
		ManagerService.getInstance().saveManager(manager);
		this.redirect(ctx, "/manager/list");
	}

	@Branch(path = "/kill", methods = { "GET", "POST" }, isLoginRequired = true)
	public void kill(ChannelHandlerContext ctx, Form form) {
		String username = form.getString("username");
		ManagerService.getInstance().removeManager(username);
		this.redirect(ctx, "/manager/list");
	}

	@Branch(path = "/login", methods = { "GET", "POST" })
	public void login(final ChannelHandlerContext ctx, Form form) {
		if (form.isGet()) {
			String encryptKey = RsaHelpers.randomRsaPublicKeyString();
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("encrypt_key", encryptKey);
			context.put("server_id", AdminServer.getInstance().getConfig()
					.getServerId());
			this.renderTemplate(ctx, "login.mus", context);
			return;
		}
		final String sessionId = ContextUtils.getSession(ctx).getSessionId();
		final String username = form.getString("username");
		String encryptKey = form.getString("encrypt_key");
		String passwordEncrypt = form.getString("password_encrypted");
		int serverId = form.getInteger("server_id");
		if (serverId != AdminServer.getInstance().getConfig().getServerId()) {
			ServiceInstance<String> service = ZkService.getInstance().getAdmin(
					"" + serverId);
			if (service == null) {
				LOG.error(String.format("service not found for server_id=%s",
						serverId));
				redirect(ctx, "/manager/login");
				return;
			}
			String url = String.format("http://%s:%s/manager/login",
					service.getAddress(), service.getPort());
			Unirest.post(url)
					.header("cookie", String.format("sid=%s", sessionId))
					.field("server_id", serverId).field("username", username)
					.field("encrypt_key", encryptKey)
					.field("password_encrypted", passwordEncrypt)
					.asStringAsync(new Callback<String>() {

						@Override
						public void completed(HttpResponse<String> response) {
							Headers headers = response.getHeaders();
							String location = headers.get("location").get(0);
							Session session = ContextUtils.getSession(ctx);
							session.setUsername(username);
							CookieHolder cookieHolder = ContextUtils.getCookie(ctx);
							cookieHolder.setSessionId(sessionId);
							redirect(ctx, location);
						}

						@Override
						public void failed(UnirestException e) {
							LOG.error("login failed", e);
							redirect(ctx, "/manager/login");
						}

						@Override
						public void cancelled() {

						}
					});
			return;
		}

		String password = RsaHelpers.decodeWithPrivate(encryptKey,
				passwordEncrypt);
		if (password == null) {
			redirect(ctx, "/manager/login");
			return;
		}
		String passwordHash = PasswordHelpers.hash(password);
		if (!ManagerService.getInstance().checkLogin(username, passwordHash)) {
			redirect(ctx, "/manager/login");
			return;
		}
		ManagerInfo manager = ManagerService.getInstance().getManager(username);
		manager.setLastLoginDate(new Date());
		ManagerService.getInstance().saveManager(manager);
		Session session = ContextUtils.getSession(ctx);
		session.setUsername(username);
		SessionService.getInstance().saveSession(session);
		CookieHolder cookieHolder = ContextUtils.getCookie(ctx);
		cookieHolder.setSessionId(session.getSessionId());
		redirect(ctx, "/");
	}

	@Branch(path = "/logout", methods = { "GET", "POST" })
	public void logout(ChannelHandlerContext ctx, Form form) {
		Session session = ContextUtils.getSession(ctx);
		if (session.getUsername() != null) {
			session.setUsername(null);
			SessionService.getInstance().saveSession(session);
		}
		CookieHolder cookies = ContextUtils.getCookie(ctx);
		cookies.clear();
		this.redirect(ctx, "/manager/login");
	}

	@Branch(path = "/list", methods = { "GET" }, isLoginRequired = true)
	public void list(ChannelHandlerContext ctx, Form form) {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("managers", ManagerService.getInstance().getManagers());
		this.renderTemplate(ctx, "managers.mus", context);
	}
}
