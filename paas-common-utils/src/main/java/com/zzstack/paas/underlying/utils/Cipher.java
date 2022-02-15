package com.zzstack.paas.underlying.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Cipher {
	
	private static Logger logger = LoggerFactory.getLogger(Cipher.class);

	public static Cipher getInstance(String algorithm) {
		Class<?> c;
		try {
			c = Class.forName("Cipher." + algorithm);
			// return (Cipher) c.newInstance();
			return (Cipher) c.getDeclaredConstructor().newInstance();
		} catch (Throwable t) {
			logger.error("Cipher: unable to load instance of '"
					+ algorithm + "'", t);
			
			return null;
		}
	}

	public byte[] encrypt(byte[] src) {
		byte[] dest = new byte[src.length];
		encrypt(src, 0, dest, 0, src.length);
		return dest;
	}

	public abstract void encrypt(byte[] src, int srcOff, byte[] dest,
			int destOff, int len);

	public byte[] decrypt(byte[] src) {
		byte[] dest = new byte[src.length];
		decrypt(src, 0, dest, 0, src.length);
		return dest;
	}

	public abstract void decrypt(byte[] src, int srcOff, byte[] dest,
			int destOff, int len);

	public abstract void setKey(byte[] key);

	public void setKey(String key) {
		setKey(key.getBytes());
	}

}
