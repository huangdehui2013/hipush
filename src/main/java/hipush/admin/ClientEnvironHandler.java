package hipush.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hipush.core.ClientEnvironStat;
import hipush.core.LocalObject;
import hipush.core.Pair;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.ReportService;
import io.netty.channel.ChannelHandlerContext;

@Root(path = "/environ")
public class ClientEnvironHandler extends BaseHandler {

	@Branch(path = "/show", methods = { "GET" }, isLoginRequired = true)
	public void showMain(ChannelHandlerContext ctx, Form form) {
		SimpleDateFormat formatter = LocalObject.dayFormatter.get();
		String day = form.getString("day", null);
		Calendar cal = Calendar.getInstance();
		if (day == null) {
			day = formatter.format(cal.getTime());
		}
		List<Pair<Integer, String>> days = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < 30; i++) {
			days.add(new Pair<Integer, String>(i, formatter.format(cal.getTime())));
			cal.add(Calendar.DATE, -1);
		}
		Collections.reverse(days);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("days", days);
		context.put("today", day);
		ClientEnvironStat environ = ReportService.getInstance().getClientEnvironStat(day);
		Map<String, Float> networks = new HashMap<String, Float>();
		networks.put("wifi", environ.getNetworkRatio(ClientEnvironStat.NETWORK_TYPE_WIFI) * 100);
		networks.put("2G", environ.getNetworkRatio(ClientEnvironStat.NETWORK_TYPE_2G) * 100);
		networks.put("3G", environ.getNetworkRatio(ClientEnvironStat.NETWORK_TYPE_3G) * 100);
		networks.put("4G", environ.getNetworkRatio(ClientEnvironStat.NETWORK_TYPE_4G) * 100);
		Map<String, Float> isps = new HashMap<String, Float>();
		isps.put("无", environ.getIspRatio(ClientEnvironStat.ISP_EMPTY) * 100);
		isps.put("移动", environ.getIspRatio(ClientEnvironStat.ISP_MOBILE) * 100);
		isps.put("联通", environ.getIspRatio(ClientEnvironStat.ISP_UNICOM) * 100);
		isps.put("电信", environ.getIspRatio(ClientEnvironStat.ISP_TELECOM) * 100);
		Map<String, Float> phones = new HashMap<String, Float>();
		for(String phone: environ.getPhoneIncrs().keySet()) {
			phones.put(phone, environ.getPhoneRatio(phone) * 100);
		}
		context.put("networks", networks.entrySet());
		context.put("isps", isps.entrySet());
		context.put("phones", phones.entrySet());
		this.renderTemplate(ctx, "client_environ.mus", context);
	}
}
