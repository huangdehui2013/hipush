package hipush.services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import hipush.core.Charsets;
import hipush.core.LocalObject;

public class EncryptService {

	private List<KeyPair> keyPairs;

	private final static EncryptService inst = new EncryptService();

	public static EncryptService getInstance() {
		return inst;
	}

	public synchronized void generateKeyPairs() {
		// 生成很慢，请异步执行，生成100个
		// 定时刷新
		KeyPairGenerator gen = LocalObject.rsaKeyGen.get();
		List<KeyPair> tempKeyPairs = new ArrayList<KeyPair>();
		for (int i = 0; i < 100; i++) {
			tempKeyPairs.add(gen.generateKeyPair());
		}
		keyPairs = tempKeyPairs;
	}

	public KeyPair randomKeyPair() {
		int index = LocalObject.random.get().nextInt(keyPairs.size());
		return keyPairs.get(index);
	}

	public byte[] encryptWithPublicKey(PublicKey encryptKey, String content) {
		Cipher cipher = LocalObject.rsaCipher.get();
		try {
			cipher.init(Cipher.ENCRYPT_MODE, encryptKey);
			return cipher.doFinal(content.getBytes(Charsets.utf8));
		} catch (Exception e) {
			return null;
		}
	}
	
	public byte[] decryptWithPrivateKey(PrivateKey decryptKey, byte[] encryptData) {
		Cipher cipher = LocalObject.rsaCipher.get();
		try {
			cipher.init(Cipher.DECRYPT_MODE, decryptKey);
			return cipher.doFinal(encryptData);
		} catch (Exception e) {
			return null;
		}
	}
	
	public byte[] encryptByDes(SecretKey secretKey, String content) {
		Cipher cipher = LocalObject.desCipher.get();
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return cipher.doFinal(content.getBytes(Charsets.utf8));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String decryptByDes(SecretKey secretKey, byte[] encryptData) {
		Cipher cipher = LocalObject.desCipher.get();
		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] bytes = cipher.doFinal(encryptData);
			return new String(bytes, Charsets.utf8);
		} catch (Exception e) {
			return null;
		}
	}
	
}
