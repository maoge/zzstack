package com.zzstack.paas.underlying.metasvr.utils;

import com.zzstack.paas.underlying.utils.MD5;

public class PasswdUtils {

    public static String generatePasswd(String accName, String passwd) {
        String concatAccPasswd = String.format("%s|%s", accName, passwd);
        return MD5.md5(concatAccPasswd);
    }

}
