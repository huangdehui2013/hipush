package hipush.http;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;

import java.util.Set;
import java.util.TreeSet;

public class CookieHolder {

	private Set<Cookie> cookies;

	public CookieHolder(Set<Cookie> cookies) {
		if (cookies.isEmpty()) {
			this.cookies = new TreeSet<Cookie>();
		} else {
			this.cookies = cookies;
			for(Cookie cookie: cookies) {
				cookie.setPath("/");
			}
		}
	}

	public String getSessionId() {
		return getValue("sid");
	}

	public void setSessionId(String sid) {
		Cookie cookie = get("sid");
		if (cookie == null) {
			cookie = new DefaultCookie("sid", sid);
			cookies.add(cookie);
		} else if (!cookie.value().equals(sid)) {
			cookie.setValue(sid);
		}
		cookie.setPath("/");
		cookie.setMaxAge(Long.MIN_VALUE);
	}

	public String getValue(String name) {
		Cookie cookie = get(name);
		if (cookie == null) {
			return null;
		}
		if(cookie.maxAge() == 0) {
			return null;
		}
		return cookie.value();
	}

	public Cookie get(String name) {
		for (Cookie cookie : cookies) {
			if (cookie.name().toLowerCase().equals(name.toLowerCase())) {
				return cookie;
			}
		}
		return null;
	}

	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}

	public void remove(String name) {
		Cookie cookie = get(name);
		if (cookie != null) {
			cookie.setMaxAge(0);
		}
	}

	public void clear() {
		for (Cookie cookie : cookies) {
			cookie.setMaxAge(0);
		}
	}

	public Set<Cookie> getCookies() {
		return cookies;
	}

}
