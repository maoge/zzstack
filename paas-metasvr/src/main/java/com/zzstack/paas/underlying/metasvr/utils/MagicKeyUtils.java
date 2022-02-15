package com.zzstack.paas.underlying.metasvr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class MagicKeyUtils {

    private static Logger logger = LoggerFactory.getLogger(MagicKeyUtils.class);

    public static String getMagicKey(RoutingContext ctx) {
        HttpServerRequest req = ctx.request();
        String key = null;
        
        if (req.headers().contains(FixHeader.HEADER_MAGIC_KEY)) {
            // for paas-sdk: "MAGIC_KEY : 32e10f19-75fb-41e0-871a-2e5773c29b06"
            key = req.getHeader(FixHeader.HEADER_MAGIC_KEY);
        } else {
            // for paas-ui: "Cookie : MAGIC_KEY=32e10f19-75fb-41e0-871a-2e5773c29b06"
            Cookie cookie = req.getCookie(FixHeader.HEADER_MAGIC_KEY);
            if (cookie == null) {
                logger.error("uri:{}, missing Cookie with MAGIC_KEY .....", req.uri());
                return null;
            }
            key = cookie.getValue();
        }
        
        if (key == null || key.isEmpty()) {
            logger.error("missing MAGIC_KEY .....");
            return null;
        }
        
        return key;
    }

}
