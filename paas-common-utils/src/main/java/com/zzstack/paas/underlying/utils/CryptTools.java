package com.zzstack.paas.underlying.utils;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class CryptTools {

    private static final String DEFAULT_PRIVATE_KEY_STRING = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAocbCrurZGbC5GArEHKlAfDSZi7gFBnd4yxOt0rwTqKBFzGyhtQLu5PRKjEiOXVa95aeIIBJ6OhC2f8FjqFUpawIDAQABAkAPejKaBYHrwUqUEEOe8lpnB6lBAsQIUFnQI/vXU4MV+MhIzW0BLVZCiarIQqUXeOhThVWXKFt8GxCykrrUsQ6BAiEA4vMVxEHBovz1di3aozzFvSMdsjTcYRRo82hS5Ru2/OECIQC2fAPoXixVTVY7bNMeuxCP4954ZkXp7fEPDINCjcQDywIgcc8XLkkPcs3Jxk7uYofaXaPbg39wuJpEmzPIxi3k0OECIGubmdpOnin3HuCP/bbjbJLNNoUdGiEmFL5hDI4UdwAdAiEAtcAwbm08bKN7pwwvyqaCBC//VnEWaq39DCzxr+Z2EIk=";
    public static final String DEFAULT_PUBLIC_KEY_STRING = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKHGwq7q2RmwuRgKxBypQHw0mYu4BQZ3eMsTrdK8E6igRcxsobUC7uT0SoxIjl1WveWniCASejoQtn/BY6hVKWsCAwEAAQ==";

    public static void main(String[] args) throws Exception {
        String src = args[0];
        System.out.println("dest:" + encrypt(src));
    }

    public static String encrypt(String plainText) throws Exception {
        return encrypt((String) null, plainText);
    }

    public static String encrypt(String key, String plainText) throws Exception {
        if (key == null) {
            key = DEFAULT_PRIVATE_KEY_STRING;
        }
        byte[] keyBytes = Base64.base64ToByteArray(key);
        return encrypt(keyBytes, plainText);
    }

    public static String encrypt(byte[] keyBytes, String plainText) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA", "SunRsaSign");
        PrivateKey privateKey = factory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
            // For IBM JDK, 原因请看解密方法中的说明
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPrivateExponent());
            Key fakePublicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, fakePublicKey);
        }

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        String encryptedString = Base64.byteArrayToBase64(encryptedBytes);

        return encryptedString;
    }

    public static String decrypt(String cipherText) throws Exception {
        return decrypt((String) null, cipherText);
    }

    public static String decrypt(String publicKeyText, String cipherText) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyText);

        return decrypt(publicKey, cipherText);
    }

    public static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            // 因为 IBM JDK 不支持私钥加密, 公钥解密, 所以要反转公私钥
            // 也就是说对于解密, 可以通过公钥的参数伪造一个私钥对象欺骗 IBM JDK
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            RSAPrivateKeySpec spec = new RSAPrivateKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
            Key fakePrivateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            // It is a stateful object. so we need to get new one.
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, fakePrivateKey);
        }

        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        byte[] cipherBytes = Base64.base64ToByteArray(cipherText);
        byte[] plainBytes = cipher.doFinal(cipherBytes);

        return new String(plainBytes);
    }

    public static PublicKey getPublicKey(String publicKeyText) {
        if (publicKeyText == null || publicKeyText.isEmpty()) {
            publicKeyText = DEFAULT_PUBLIC_KEY_STRING;
        }

        try {
            byte[] publicKeyBytes = Base64.base64ToByteArray(publicKeyText);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SunRsaSign");
            return keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get public key", e);
        }
    }

}
