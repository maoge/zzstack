package com.zzstack.paas.underlying.utils;

import java.math.BigInteger;

public final class DES3 extends Cipher {
	static {
	}

	DES des1 = new DES();
	DES des2 = new DES();
	DES des3 = new DES();

	@Override
	public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
		des1.encrypt(src, srcOff, dest, destOff, len);
		des2.decrypt(dest, destOff, dest, destOff, len);
		des3.encrypt(dest, destOff, dest, destOff, len);
	}

	@Override
	public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
		des3.decrypt(src, srcOff, dest, destOff, len);
		des2.encrypt(dest, destOff, dest, destOff, len);
		des1.decrypt(dest, destOff, dest, destOff, len);
	}

	@Override
	public void setKey(byte[] key) {
		byte[] subKey = new byte[8];
		des1.setKey(key);
		System.arraycopy(key, 8, subKey, 0, 8);
		des2.setKey(subKey);
		System.arraycopy(key, 16, subKey, 0, 8);
		des3.setKey(subKey);
	}

	static byte[] key = { (byte) 0x12, (byte) 0x34, (byte) 0x45, (byte) 0x78, (byte) 0x87, (byte) 0x34,
			(byte) 0x43, (byte) 0x23, (byte) 0x89, (byte) 0x55, (byte) 0x01, (byte) 0x77, (byte) 0x87,
			(byte) 0xef, (byte) 0x43, (byte) 0x78, (byte) 0xcd, (byte) 0x65, (byte) 0x9a, (byte) 0x21,
			(byte) 0x12, (byte) 0xab, (byte) 0x56, (byte) 0x78, };

	static public String decrypt24(String source) {
		// 1.先进行DES3的解密
		byte[] txt = new byte[24];
		BigInteger t = new BigInteger(source, 16); // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
		byte[] b = t.toByteArray();

		if (b[0] == 0) { // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
			System.arraycopy(b, 1, txt, 0, b.length - 1);
		} else {
			System.arraycopy(b, 0, txt, 0, b.length);
		}

		byte[] dec;
		DES3 cipher = new DES3();
		cipher.setKey(key);
		dec = cipher.decrypt(txt);

		byte[] dect = new byte[dec.length];
		int j = 0;
		for (int i = 0; i < dec.length; i++) {
			if (dec[i] > 0) {
				dect[j++] = dec[i];
			}
		}
		String rt = (new String(dect, 0, j));

		// 2.再进行 凯撒算法 的解密
		// rt=DesEncrypterIsmp.decrypt(rt);
		return rt;// new String(dec);
	}

	static public String decrypt(String source) {
		// 1.先进行DES3的解密
		byte[] txt = new byte[32];
		BigInteger t = new BigInteger(source, 16); // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
		byte[] b = t.toByteArray();

		if (b[0] == 0) { // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
			System.arraycopy(b, 1, txt, 0, b.length - 1);
		} else {
			System.arraycopy(b, 0, txt, 0, b.length);
		}

		byte[] dec;
		DES3 cipher = new DES3();
		cipher.setKey(key);
		dec = cipher.decrypt(txt);

		byte[] dect = new byte[dec.length];
		int j = 0;
		for (int i = 0; i < dec.length; i++) {
			if (dec[i] > 0) {
				dect[j++] = dec[i];
			}
		}
		String rt = (new String(dect, 0, j));

		// 2.再进行 凯撒算法 的解密
		// rt=DesEncrypterIsmp.decrypt(rt);
		return rt;// new String(dec);
	}

	static public String decrypt48(String source) {
		// 1.先进行DES3的解密
		byte[] txt = new byte[48];
		BigInteger t = new BigInteger(source, 16); // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
		byte[] b = t.toByteArray();

		if (b[0] == 0) { // 如果首字符为>8,则解释成16进制多一个字节，第一个字节为0，可以废弃。
			System.arraycopy(b, 1, txt, 0, b.length - 1);
		} else {
			System.arraycopy(b, 0, txt, 0, b.length);
		}

		byte[] dec;
		DES3 cipher = new DES3();
		cipher.setKey(key);
		dec = cipher.decrypt(txt);

		byte[] dect = new byte[dec.length];
		int j = 0;
		for (int i = 0; i < dec.length; i++) {
			if (dec[i] > 0) {
				dect[j++] = dec[i];
			}
		}
		String rt = (new String(dect, 0, j));

		return rt;
	}

	static public void displayString(String source) {
		byte[] aa = source.getBytes();
		System.out.print("BEGIN:");
		for (int i = 0; i < aa.length; i++) {
			System.out.print(aa[i]);
			System.out.print(" ");
		}
		System.out.println(":END");
	}

	static public String encrypt24(String source) {
		// 1.先进行凯撒算法的加密
		// source=DesEncrypterIsmp.encode(source);

		// 2.再进行传统DES3的加密
		byte[] txt = new byte[24];
		byte[] b = source.getBytes();

		System.arraycopy(b, 0, txt, 0, b.length);

		byte[] enc;
		DES3 cipher = new DES3();
		cipher.setKey(key);

		enc = cipher.encrypt(txt);
		return printHex(enc);
	}

	static public String encrypt(String source) {
		// 1.先进行凯撒算法的加密
		// source=DesEncrypterIsmp.encode(source);

		// 2.再进行传统DES3的加密
		byte[] txt = new byte[32];
		byte[] b = source.getBytes();

		System.arraycopy(b, 0, txt, 0, b.length);

		byte[] enc;
		DES3 cipher = new DES3();
		cipher.setKey(key);

		enc = cipher.encrypt(txt);
		return printHex(enc);
	}

	static public String encrypt48(String source) {
		// 1.先进行凯撒算法的加密
		// source=DesEncrypterIsmp.encode(source);

		// 2.再进行传统DES3的加密
		byte[] txt = new byte[48];
		byte[] b = source.getBytes();

		System.arraycopy(b, 0, txt, 0, b.length);

		byte[] enc;
		DES3 cipher = new DES3();
		cipher.setKey(key);

		enc = cipher.encrypt(txt);
		return printHex(enc);
	}

	static String printHex(byte[] buf) {
		byte[] out = new byte[buf.length + 1];
		out[0] = 0;
		System.arraycopy(buf, 0, out, 1, buf.length);
		BigInteger big = new BigInteger(out);
		return big.toString(16);
	}

	static String printHex(int i) {
		BigInteger b = BigInteger.valueOf(i + 0x100000000L);
		BigInteger c = BigInteger.valueOf(0x100000000L);
		if (b.compareTo(c) != -1)
			b = b.subtract(c);
		return b.toString(16);
	}

	public static void main(String[] args) {
		if (args != null && args.length == 2) {
		    String arg1 = args[0];
		    String arg2 = args[1];

		    if ("encode".equalsIgnoreCase(arg1)) {
		        System.out.println("************加密*************");
		        System.out.println(encrypt(arg2));
			} else if("decode".equalsIgnoreCase(arg1)) {
			    System.out.println("************解密*************");
			    System.out.println(decrypt(arg2));
			} else {
			    throw new IllegalArgumentException("first param must be encode or decode");
			}
		} else {
		    String str2encypt = "abcd.1234";
		    String enStr = encrypt(str2encypt);
		    System.out.println("************加密*************");
		    System.out.println(enStr);
		    
		    String str2decypt = enStr;
		    String deStr = decrypt(str2decypt);	    
		    System.out.println("************解密*************");
		    System.out.println(deStr);
		}

	}

}
