package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import io.vertx.core.json.JsonObject;

public class OracleDbDeployerUtils {


    public static boolean deployOrclInst(JsonObject jsonOrclInst, String logKey, String magicKey, ResultBean result) {
        String instId = jsonOrclInst.getString(FixHeader.HEADER_INST_ID);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;
        DeployLog.pubLog(logKey, "start oracle instance ......");

        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;
        DeployLog.pubLog(logKey, "init oracle instance success ......");

        return true;
    }

    public static boolean unDeployOrclInst(JsonObject jsonOrclInst, String logKey, String magicKey, ResultBean result) {

        String instId = jsonOrclInst.getString(FixHeader.HEADER_INST_ID);
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) return false;

        return true;
    }


}
