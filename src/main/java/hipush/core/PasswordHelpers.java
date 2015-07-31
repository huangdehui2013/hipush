package hipush.core;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Base64;

public class PasswordHelpers {

	private final static String SALT = "hipushisgreat";

	public static String hash(String password) {
		MessageDigest digest = LocalObject.md5.get();
		Base64 b64 = LocalObject.base64.get();
		return b64.encodeToString(digest.digest((password + SALT)
				.getBytes(Charsets.utf8)));
	}

}
