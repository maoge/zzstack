package com.zzstack.paas.underlying.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5 {

    public static String md5(byte[] source) {
        return DigestUtils.md5Hex(source);
    }

    /**
     * 采用MD5算法加密字符串.
     *
     * @param str 需要加密的字符串
     * @return 加密后的字符串
     */
    public static String md5(final String str) {
        return DigestUtils.md5Hex(str);
    }

}
