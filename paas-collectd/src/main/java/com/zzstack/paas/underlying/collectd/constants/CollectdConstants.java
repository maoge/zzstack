package com.zzstack.paas.underlying.collectd.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException.SdkErrInfo;

public class CollectdConstants {
    
    public static Logger logger = LoggerFactory.getLogger(CollectdConstants.class);
    
    public static String uuid;
    public static String servInstID;
    public static String metaSvrUrl;
    public static String metaSvrUsr;
    public static String metaSvrPasswd;
    public static int collectdPort;

    public static void init() throws PaasSdkException {
        // 在启动脚本中设置如下环境变量
        uuid = System.getProperties().getProperty(FixHeader.HEADER_UUID);
        if (uuid == null || uuid.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_UUID);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        
        servInstID = System.getProperties().getProperty(FixHeader.HEADER_SERV_INST_ID);
        if (servInstID == null || servInstID.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_SERV_INST_ID);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        
        metaSvrUrl = System.getProperties().getProperty(FixHeader.HEADER_META_SVR_URL);
        if (metaSvrUrl == null || metaSvrUrl.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_META_SVR_URL);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        
        metaSvrUsr = System.getProperties().getProperty(FixHeader.HEADER_META_SVR_USR);
        if (metaSvrUsr == null || metaSvrUsr.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_META_SVR_USR);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        
        metaSvrPasswd = System.getProperties().getProperty(FixHeader.HEADER_META_SVR_PASSWD);
        if (metaSvrPasswd == null || metaSvrPasswd.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_META_SVR_PASSWD);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        
        String strCollectdPort = System.getProperties().getProperty(FixHeader.HEADER_COLLECTD_PORT);
        if (strCollectdPort == null || strCollectdPort.isEmpty()) {
            logger.error(CONSTS.ERR_MISSING_COLLECTD_PORT);
            throw new PaasSdkException(SdkErrInfo.e80060001);
        }
        collectdPort = Integer.valueOf(strCollectdPort);
    }

}