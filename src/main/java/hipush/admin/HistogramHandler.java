package hipush.admin;

import hipush.core.Helpers;
import hipush.core.LocalObject;
import hipush.core.Pair;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.MeasureService;
import io.netty.channel.ChannelHandlerContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Root(path = "/hist")
public class HistogramHandler extends BaseHandler {

	@Branch(path = "/main/list", methods = { "GET" }, isLoginRequired = true)
	public void listMain(ChannelHandlerContext ctx, Form form) {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("items", MeasureService.getInstance().getMainKeys());
		this.renderTemplate(ctx, "perf_mains.mus", context);
	}

	@Branch(path = "/io/list", methods = { "GET" }, isLoginRequired = true)
	public void listIO(ChannelHandlerContext ctx, Form form) {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("items", MeasureService.getInstance().getIOKeys());
		this.renderTemplate(ctx, "perf_ios.mus", context);
	}

	@Branch(path = "/main/detail", methods = { "GET" }, isLoginRequired = true)
	public void showMain(ChannelHandlerContext ctx, Form form) {
		SimpleDateFormat formatter = LocalObject.dayFormatter.get();
		String day = form.getString("day", null);
		String key = form.getString("key");
		Calendar cal = Calendar.getInstance();
		if (day == null) {
			day = formatter.format(cal.getTime());
		}
		List<Pair<Integer, String>> days = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < 30; i++) {
			days.add(new Pair<Integer, String>(i, formatter.format(cal
					.getTime())));
			cal.add(Calendar.DATE, -1);
		}
		Collections.reverse(days);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("days", days);
		context.put("today", day);
		context.put("key", key);
		Map<Long, Long> hist = MeasureService.getInstance()
				.getMainHistgramByKey(day, key);
		int year = Integer.parseInt(day.substring(0, 4));
		int month = Integer.parseInt(day.substring(4, 6));
		int date = Integer.parseInt(day.substring(6));
		cal.set(year, month - 1, date);
		Date now = cal.getTime();
		long beginTs = Helpers.getStartOfDay(now).getTime();
		long endTs = Helpers.getEndOfDay(now).getTime();
		long beginMin = beginTs / (60 * 1000);
		long endMin = endTs / (60 * 1000);
		StringBuffer histValues = new StringBuffer();
		for (long i = beginMin; i <= endMin; i += 1) {
			Long total = hist.get(i);
			if (total == null) {
				histValues.append(0);
			} else {
				histValues.append(total);
			}
			if (i < endTs) {
				histValues.append(',');
			}
		}
		context.put("begin_ts", beginTs);
		context.put("hist_values", histValues.toString());
		this.renderTemplate(ctx, "perf_main_detail.mus", context);
	}

	@Branch(path = "/io/detail", methods = { "GET" }, isLoginRequired = true)
	public void showIO(ChannelHandlerContext ctx, Form form) {
		SimpleDateFormat formatter = LocalObject.dayFormatter.get();
		String day = form.getString("day", null);
		String key = form.getString("key");
		Calendar cal = Calendar.getInstance();
		if (day == null) {
			day = formatter.format(cal.getTime());
		}
		List<Pair<Integer, String>> days = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < 30; i++) {
			days.add(new Pair<Integer, String>(i, formatter.format(cal
					.getTime())));
			cal.add(Calendar.DATE, -1);
		}
		Collections.reverse(days);
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("days", days);
		context.put("today", day);
		context.put("key", key);
		Map<Long, Long> hist = MeasureService.getInstance().getIOHistgramByKey(
				day, key);
		int year = Integer.parseInt(day.substring(0, 4));
		int month = Integer.parseInt(day.substring(4, 6));
		int date = Integer.parseInt(day.substring(6));
		cal.set(year, month - 1, date);
		Date now = cal.getTime();
		long beginTs = Helpers.getStartOfDay(now).getTime();
		long endTs = Helpers.getEndOfDay(now).getTime();
		long beginMin = beginTs / (60 * 1000);
		long endMin = endTs / (60 * 1000);
		StringBuffer histValues = new StringBuffer();
		for (long i = beginMin; i <= endMin; i += 1) {
			Long total = hist.get(i);
			if (total == null) {
				histValues.append(0);
			} else {
				histValues.append(total);
			}
			if (i < endTs) {
				histValues.append(',');
			}
		}
		context.put("begin_ts", beginTs);
		context.put("hist_values", histValues.toString());
		this.renderTemplate(ctx, "perf_io_detail.mus", context);
	}
}
