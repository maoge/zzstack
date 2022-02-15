package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.metasvr.bean.AccountSessionBean;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.MagicKeyUtils;

import io.vertx.ext.web.RoutingContext;

public class MetaSvrAuthHandler implements IAuthHandler {

    @Override
    public boolean doAuth(RoutingContext ctx) {
        String key = MagicKeyUtils.getMagicKey(ctx);
        if (key == null)
            return false;
        
        AccountSessionBean accSession = MetaSvrGlobalRes.get().getCmptMeta().getSessionByMagicKey(key);
        if (accSession == null)
            return false;
        
        return accSession.isSessionValid();
    }

}
