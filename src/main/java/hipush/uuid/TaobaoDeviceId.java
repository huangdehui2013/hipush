package hipush.uuid;


public class TaobaoDeviceId {

	public static boolean isValid(String id) {
		if (id == null || id.length() != 64) {
			return false;
		}
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			if (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A'
					&& c <= 'Z') {
				continue;
			}
			return false;
		}
		return true;
	}	
}
