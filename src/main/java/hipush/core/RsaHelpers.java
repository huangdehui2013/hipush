package hipush.core;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

public class RsaHelpers {

	private static final int RSAKEYS_SIZE = 10;
	private static Map<PublicKey, PrivateKey> rsaKeys = new HashMap<PublicKey, PrivateKey>(RSAKEYS_SIZE);
	private static Map<String, PublicKey> rsaPublicKeyMap = new HashMap<String, PublicKey>(RSAKEYS_SIZE);
	private static List<PublicKey> rsaPublicKeys = new ArrayList<PublicKey>(RSAKEYS_SIZE);
	private static long lastTs;

	public static PublicKey randomRsaPublicKey() {
		if (isKeyExpired()) {
			synchronized (rsaKeys) {
				if (isKeyExpired()) {
					KeyPairGenerator gen = LocalObject.rsaKeyGen.get();
					for (int i = 0; i < RSAKEYS_SIZE; i++) {
						KeyPair pair = gen.generateKeyPair();
						rsaKeys.put(pair.getPublic(), pair.getPrivate());
						rsaPublicKeys.add(pair.getPublic());
						Base64 b64 = LocalObject.base64.get();
						String pubKey = b64.encodeToString(pair.getPublic().getEncoded());
						rsaPublicKeyMap.put(pubKey, pair.getPublic());
					}
					lastTs = System.currentTimeMillis();
				}
			}
		}
		Random random = LocalObject.random.get();
		int index = random.nextInt(RSAKEYS_SIZE);
		return rsaPublicKeys.get(index);
	}

	public static boolean isKeyExpired() {
		if (rsaKeys.isEmpty() || lastTs == 0) {
			return true;
		}
		if (System.currentTimeMillis() - lastTs > 7200) {
			return true;
		}
		return false;
	}

	public static String randomRsaPublicKeyString() {
		PublicKey key = randomRsaPublicKey();
		Base64 b64 = LocalObject.base64.get();
		return b64.encodeToString(key.getEncoded());
	}

	public static String encodeWithPublic(PublicKey publicKey, String content) {
		Cipher cipher = LocalObject.rsaCipher.get();
		try {
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			Base64 b64 = LocalObject.base64.get();
			return b64.encodeToString(cipher.doFinal(content.getBytes(Charsets.utf8)));
		} catch (Exception e) {
			return null;
		}
	}

	public static String decodeWithPrivate(String publicKey, String contentEncrypted) {
		PublicKey pubKey = rsaPublicKeyMap.get(publicKey);
		if (pubKey == null) {
			return null;
		}
		return decodeWithPrivate(pubKey, contentEncrypted);
	}

	public static String decodeWithPrivate(PublicKey publicKey, String contentEncrypted) {
		Cipher cipher = LocalObject.rsaCipher.get();
		PrivateKey key = rsaKeys.get(publicKey);
		Base64 b64 = LocalObject.base64.get();
		byte[] bytesEncrypted = b64.decode(contentEncrypted.getBytes(Charsets.utf8));
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] bytes = cipher.doFinal(bytesEncrypted);
			return new String(bytes, Charsets.utf8);
		} catch (Exception e) {
			return null;
		}
	}

}
