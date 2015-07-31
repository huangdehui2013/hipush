package hipush.web;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.UserInfo;
import hipush.services.UserService;
import hipush.uuid.ClientId;
import hipush.uuid.TaobaoDeviceId;
import hipush.uuid.TokenId;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

@Root(path = "/user")
public class UserHandler extends BaseHandler {
	
	@Branch(path = "/genId", methods = { "GET", "POST" })
	public void genId(ChannelHandlerContext ctx, Form form) {
		String deviceId = form.getString("device_id").toLowerCase();
		String appKey = form.getString("app_key").toLowerCase();
		
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise(String.format("appkey %s not exists", appKey));
			return;
		}
		if (!TaobaoDeviceId.isValid(deviceId)) {
			form.raise(String.format("device_id not illegal", appKey));
			return;
		}
		String clientId = UserService.getInstance().getClientId(deviceId,
				appKey);
		if (clientId == null) {
			clientId = ClientId.nextId();
			UserService.getInstance().saveClientId(deviceId, appKey, clientId);
		}
		Map<String, String> result = new HashMap<String, String>(1);
		result.put("client_id", clientId);
		this.renderOk(ctx, result);
	}

	@Branch(path = "/token", methods = { "GET", "POST" })
	public void getToken(ChannelHandlerContext ctx, Form form) {
		String clientId = form.getString("client_id").toLowerCase();
		UserInfo client = UserService.getInstance().getClient(clientId);
		if(client == null) {
			form.raise(String.format("client_id %s not illegal", clientId));
			return;
		}
		String token = TokenId.nextId();
		UserService.getInstance().saveToken(token, clientId);
		Map<String, String> result = new HashMap<String, String>(1);
		result.put("token", token);
		this.renderOk(ctx, result);
	}
	
}
