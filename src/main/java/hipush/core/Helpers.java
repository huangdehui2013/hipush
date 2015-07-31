package hipush.core;

import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;

public class Helpers {

	public static String join(String sep, String[] values) {
		return join(sep, values, 0, values.length);
	}

	public static String join(String sep, String[] values, int start, int end) {
		StringBuffer buffer = new StringBuffer();
		start = Math.max(0, start);
		end = Math.min(values.length, end);
		for (int i = start; i < end; i++) {
			buffer.append(values[i]);
			if (i < values.length - 1) {
				buffer.append(sep);
			}
		}
		return buffer.toString();
	}

	public static boolean isClientTopic(String topic) {
		if (!isValidTopic(topic)) {
			return false;
		}
		return topic.charAt(0) == '@';
	}

	public static boolean isServerTopic(String topic) {
		if (!isValidTopic(topic)) {
			return false;
		}
		return topic.charAt(0) == '$';
	}

	public static boolean isValidTopic(String topic) {
		if (topic == null || topic.isEmpty()) {
			return false;
		}
		if (topic.length() > 256) {
			return false;
		}
		for (int i = 0; i < topic.length(); i++) {
			char c = topic.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '/' || c == '_'
					|| c == '-' || c == '@' || c == '$')) {
				return false;
			}
		}
		return true;
	}

	public static int hash(String key, int size) {
		MessageDigest digest = LocalObject.md5.get();
		digest.reset();
		digest.update(key.getBytes());
		byte[] bs = digest.digest();
		return ((int) (bs[0] & 0xFF) << 4 | (int) (bs[1] & 0xFF)) % size;
	}

	private final static Set<String> exts = new HashSet<String>(Arrays.asList(
			"js", "min.js", "css", "min.css", "css.map", "jpg", "png", "ico",
			"jpeg", "gif", "map", "eot", "svg", "ttf", "woff", "woff2"));

	private final static Pattern FILE_NAME_PATTERN = Pattern
			.compile("^[0-9a-zA-Z\\/\\-\\_]+$");

	public static boolean isStaticFile(String path) {
		// 严格的文件名校验
		String[] parts = path.split("\\.", 2);
		if (parts.length != 2) {
			return false;
		}
		String name = parts[0];
		String ext = parts[1];
		if (!exts.contains(ext)) {
			return false;
		}
		if (name.isEmpty()) {
			return false;
		}
		if (!FILE_NAME_PATTERN.matcher(name).find()) {
			return false;
		}
		return true;
	}

	public static String getFile(String uri) {
		URL url = Helpers.class.getClassLoader().getResource(uri);
		return url.getPath();
	}

	private final static ThreadLocal<SimpleDateFormat> formats = new ThreadLocal<SimpleDateFormat>();

	public static String formatDate(Date d) {
		SimpleDateFormat format = formats.get();
		if (format == null) {
			format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formats.set(format);
		}
		return format.format(d);
	}

	public static class Pager {

		private int currentPage;
		private int pageSize;
		private int total;
		private int maxItems = 10;
		private String urlPattern;

		public Pager(int pageSize, int total) {
			this.pageSize = pageSize;
			this.total = total;
		}

		public int offset() {
			return currentPage * pageSize;
		}

		public int limit() {
			return pageSize;
		}

		public int page() {
			return (total + pageSize - 1) / pageSize;
		}

		public int maxItems() {
			return maxItems;
		}

		public int currentPage() {
			return currentPage;
		}

		public void setCurrentPage(int page) {
			this.currentPage = page;
		}

		public void setUrlPattern(String urlPattern) {
			this.urlPattern = urlPattern;
		}

		public int total() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public boolean visable() {
			return this.total > pageSize;
		}

		public boolean first() {
			int startPage = Math.max(0, currentPage - maxItems / 2);
			return startPage > 0;
		}

		public PageItem firstItem() {
			String url = String.format(urlPattern, pageSize, 0);
			return new PageItem("首页", url, false);
		}

		public boolean end() {
			int startPage = Math.max(0, currentPage - maxItems / 2);
			return startPage + maxItems < page();
		}

		public PageItem endItem() {
			String url = String.format(urlPattern, pageSize - 1, page());
			return new PageItem("末页", url, false);
		}

		public List<PageItem> pages() {
			List<PageItem> pages = new ArrayList<PageItem>();
			int startPage = Math.max(0, currentPage - maxItems / 2);
			int maxPage = page();
			for (int i = startPage; i < startPage + maxItems && i < maxPage; i++) {
				boolean disabled = i == currentPage;
				String url = String.format(urlPattern, pageSize, i);
				PageItem item = new PageItem("" + (i + 1), url, disabled);
				pages.add(item);
			}
			return pages;
		}

		static class PageItem {

			public PageItem(String label, String url, boolean disabled) {
				this.label = label;
				this.url = url;
				this.disabled = disabled;
			}

			private String label;
			private boolean disabled;
			private String url;

			public String getLabel() {
				return label;
			}

			public boolean isDisabled() {
				return disabled;
			}

			public String getClazz() {
				return disabled ? "disabled" : "active";
			}

			public String getUrl() {
				return url;
			}

		}

	}

	public static Date getStartOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public static Date getEndOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	public static String signParams(String salt, String... params) {
		StringBuffer whole = new StringBuffer();
		for (int i = 0; i < params.length; i++) {
			whole.append(params[i]);
			if (i < params.length - 1) {
				whole.append('&');
			}
		}
		whole.append(salt);
		MessageDigest digest = LocalObject.md5.get();
		return Hex.encodeHexString((digest.digest(whole.toString().getBytes(
				Charsets.utf8))));
	}

	public static boolean checkSign(String salt, String sign, String... params) {
		return sign.equals(signParams(salt, params));
	}

}