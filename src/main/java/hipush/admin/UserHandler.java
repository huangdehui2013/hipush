package hipush.admin;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.UserService;
import hipush.uuid.ClientId;
import hipush.uuid.TaobaoDeviceId;
import hipush.uuid.TokenId;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;

@Root(path = "/user")
public class UserHandler extends BaseHandler {

	@Branch(path = "/token", methods = { "GET", "POST" })
	public void getToken(ChannelHandlerContext ctx, Form form) {
		// 如果将安全机制委托到第三方，就让第三方应用调用此接口获取用户的token
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
		}
		String token = TokenId.nextId();
		UserService.getInstance().saveClientId(deviceId, appKey, clientId);
		UserService.getInstance().saveToken(token, clientId);
		Map<String, String> result = new HashMap<String, String>(4);
		result.put("client_id", clientId);
		result.put("token", token);
		this.setKeepAlive(ctx, true);
		this.renderOk(ctx, result);
	}
}
