package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MinioDeployerUtils {
    
    public static String getEndpoints(JsonArray minioArr) {
        if (minioArr == null || minioArr.isEmpty())
            return "";
        
        StringBuilder sb = new StringBuilder();
        int size = minioArr.size();
        for (int i = 0; i < size; i++) {
            JsonObject minio = minioArr.getJsonObject(i);
            
            String sshID = minio.getString(FixHeader.HEADER_SSH_ID);
            String mount = minio.getString(FixHeader.HEADER_MINIO_MOUNT);
            String[] mountArr = mount.split(CONSTS.PATH_COMMA);
            
            PaasSsh ssh = DeployUtils.getSshById(sshID);
            String ip = ssh.getServerIp();
            
            // http://192.168.238.135/data{1...4}
            for (String mountPoint : mountArr) {
                String endPoint = String.format("http://%s%s", ip, mountPoint);
                
                if (sb.length() > 0) {
                    sb.append("\\\\r");
                }
                
                sb.append(endPoint);
            }
        }
        
        
        return sb.toString();
    }
    
    public static boolean deployMinioNode(JsonObject minioNode, String endpoints, String version, String logKey, String magicKey, ResultBean result) {
        String instId = minioNode.getString(FixHeader.HEADER_INST_ID);
        String sshId = minioNode.getString(FixHeader.HEADER_SSH_ID);
        String minioPort = minioNode.getString(FixHeader.HEADER_PORT);
        String minioConsolePort = minioNode.getString(FixHeader.HEADER_CONSOLE_PORT);
        String region = minioNode.getString(FixHeader.HEADER_MINIO_REGION);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;
        
        PaasSsh ssh = DeployUtils.getSshById(sshId);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        String address = String.format("%s:%s", servIp, minioPort);
        String consoleAddress = String.format("%s:%s", servIp, minioConsolePort);
        
        {
            String info = String.format("deploy minio: %s:%s, instId:%s", servIp, minioPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "minio", instId, servIp, minioPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("minio.PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeploy(ssh2, "minio", instId, servIp, minioConsolePort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("minio.CONSOLE_PORT is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.STORE_MINIO_FILE_ID, FixDefs.STORE_MINIO_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.STORE_MINIO_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + minioPort;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // export MINIO_REGION_NAME=%MINIO_REGION%
        // export MINIO_ACCESS_KEY=%MINIO_USER%
        // export MINIO_SECRET_KEY=%MINIO_PASSWD%
        // nohup ./bin/minio server --address %ADDRESS% --console-address %CONSOLE_ADDRESS% --config-dir ./etc \
        // %ENDPOINTS%
        String file = FixDefs.START_SHELL;
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MINIO_REGION, region, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MINIO_USER, CONSTS.MINIO_ACCESS_KEY, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MINIO_PASSWD, CONSTS.MINIO_SECRET_KEY, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADDRESS, address, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CONSOLE_ADDRESS, consoleAddress, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ENDPOINTS, endpoints, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start minio ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "minio", instId, servIp, minioPort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update t_meta_instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        return true;
    }
    
    public static boolean undeployMinioNode(JsonObject minioNode, String version, String logKey, String magicKey, ResultBean result) {
        String instId = minioNode.getString(FixHeader.HEADER_INST_ID);
        String sshId = minioNode.getString(FixHeader.HEADER_SSH_ID);
        String minioPort = minioNode.getString(FixHeader.HEADER_PORT);
        String mount = minioNode.getString(FixHeader.HEADER_MINIO_MOUNT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("start undeploy minio: %s:%s, instId:%s", servIp, minioPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.STORE_MINIO_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);
        
        String newName = oldName + "_" + minioPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.STORE_MINIO_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop minio ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortDown(ssh2, "minio", instId, servIp, minioPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // clear mount points
        String[] mountArr = mount.split(CONSTS.PATH_COMMA);
        for (String mountPoint : mountArr) {
            String path = String.format("%s/*", mountPoint);
            DeployUtils.rm(ssh2, path, logKey, result);
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }

}
