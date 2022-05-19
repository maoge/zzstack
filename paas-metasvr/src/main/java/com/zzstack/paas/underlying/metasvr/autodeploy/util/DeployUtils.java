package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.pulsar.shade.org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasDeployHost;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.exception.SSHException;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.StringUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DeployUtils {

    private static final Logger logger = LoggerFactory.getLogger(DeployUtils.class);
    
    public static TopoResult LoadServTopo(String servInstID, String logKey, boolean checkFlag, ResultBean result) {
        JsonObject topoJson = new JsonObject();
        if (!getServiceTopo(topoJson, servInstID, logKey, result)) {
            return new TopoResult(null, "", false);
        }

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        if (checkFlag) {
            if (isServiceDeployed(serv, logKey, result)) {
                return new TopoResult(null, "", false);
            }
        } else {
            if (isServiceNotDeployed(serv, logKey, result)) {
                return new TopoResult(null, "", false);
            }
        }

        PaasInstance servInst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt servCmpt = cmptMeta.getCmptById(servInst.getCmptId());
        String version = serv.getVersion();

        JsonObject servJson = topoJson.getJsonObject(servCmpt.getCmptName());
        return new TopoResult(servJson, version, true);
    }
    
    public static boolean postProc(String servInstID, String flag, String logKey, String magicKey,
            ResultBean result) {

        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        String info;
        if (flag.equals(FixDefs.STR_TRUE)) {
            info = String.format("service inst_id: %s, deploy sucess ......", servInstID);
        } else {
            info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        }
        DeployLog.pubSuccessLog(logKey, info);

        return true;
    }
    
    public static void postDeployLog(boolean isOk, String servInstID, String logKey, String flag) {
        if (isOk) {
            String info = String.format("service inst_id:%s, %s sucess ......", servInstID, flag);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("service inst_id:%s, %s failed ......", servInstID, flag);
            DeployLog.pubFailLog(logKey, info);
        }
    }

    public static JsonObject getSpecifiedOrclInst(JsonArray dgContainer, String instID) {
        for (int j = 0; j < dgContainer.size(); j++) {
            JsonArray orclInst = dgContainer.getJsonObject(j).getJsonArray(FixHeader.HEADER_ORCL_INSTANCE);
            for (int i = 0; i < orclInst.size(); i++) {
                JsonObject jsonOrclInst = orclInst.getJsonObject(i);
                if (instID.equals(jsonOrclInst.getString(FixHeader.HEADER_INST_ID))) {
                    return jsonOrclInst;
                }
            }
        }
        return null;
    }
    
    public static boolean getServiceTopo(JsonObject jsonTopo, final String servInstID, final String logKey,
                                         ResultBean result) {

        if (!MetaDataDao.loadServiceTopo(jsonTopo, servInstID)) {
            String retInfo = jsonTopo.getString(FixHeader.HEADER_RET_INFO);
            logger.error("{}", retInfo);
            DeployLog.pubFailLog(logKey, retInfo);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(retInfo);

            return false;
        }

        return true;
    }
    
    public static boolean etcdCheckBeforeDeploy(PaasService serv, JsonArray etcdNodeArr, JsonArray apiSixNodeArr, String logKey) {
        // 先判断是否是生产环境,生产环境的话etcd必须至少是三个节点的集群,开发、测试环境部署单节点或者集群的etcd
        boolean isNotEnoughProductCondition = serv.isProduct() && etcdNodeArr.size() < FixHeader.ETCD_PRODUCT_ENV_MIN_NODES;
        if (isNotEnoughProductCondition) {
            DeployLog.pubErrorLog(logKey, FixDefs.ERR_ETCD_NODE_REQUIRED_CLUSTER);
            return false;
        }
        // etcd的节点不能小于1
        if (etcdNodeArr.size() < 1) {
            DeployLog.pubErrorLog(logKey, FixDefs.ERR_ETCD_NODE_LESS_THAN_ONE);
            return false;
        }
        
        return true;
    }

    public static PaasService getService(final String instID, final String logKey, ResultBean result) {
        PaasService service = MetaSvrGlobalRes.get().getCmptMeta().getService(instID);

        if (service == null) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_SERVICE_NOT_FOUND);

            logger.error("{}, inst_id:{}", CONSTS.ERR_SERVICE_NOT_FOUND, instID);
            DeployLog.pubFailLog(logKey, CONSTS.ERR_SERVICE_NOT_FOUND);

            return null;
        }

        return service;
    }

    public static PaasInstance getInstance(final String instID, final String logKey, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance instance = meta.getInstance(instID);

        if (instance == null) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_FOUND);

            logger.error("{}, inst_id:{}", CONSTS.ERR_INSTANCE_NOT_FOUND, instID);
            DeployLog.pubFailLog(logKey, CONSTS.ERR_INSTANCE_NOT_FOUND);

            return null;
        }

        return instance;
    }

    public static boolean isServiceDeployed(final PaasService serv, final String logKey, ResultBean result) {
        if (serv != null && serv.isDeployed()) {
            logger.info("{}", CONSTS.ERR_SERVICE_ALLREADY_DEPLOYED);
            DeployLog.pubSuccessLog(logKey, CONSTS.ERR_SERVICE_ALLREADY_DEPLOYED);

            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(CONSTS.ERR_SERVICE_ALLREADY_DEPLOYED);

            return true;
        }

        return false;
    }

    public static boolean isServiceNotDeployed(final PaasService serv, final String logKey, ResultBean result) {
        if (serv != null && !serv.isDeployed()) {
            logger.info("service inst_id:{}, {}", serv.getInstId(), CONSTS.ERR_SERVICE_NOT_DEPLOYED);
            DeployLog.pubSuccessLog(logKey, CONSTS.ERR_SERVICE_NOT_DEPLOYED);

            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(CONSTS.ERR_SERVICE_NOT_DEPLOYED);

            return true;
        }

        return false;
    }

    public static boolean isInstanceDeployed(final PaasInstance inst, final String logKey, ResultBean result) {
        if (inst != null && inst.isDeployed()) {
            String info = String.format("instance is allready deployed, inst_id:%s", inst.getInstId());

            logger.info("{}", info);
            DeployLog.pubSuccessLog(logKey, info);

            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_ALLREADY_DEPLOYED);
            return true;
        }

        return false;
    }

    public static boolean isInstanceNotDeployed(final PaasInstance inst, final String logKey, ResultBean result) {
        if (inst != null && !inst.isDeployed()) {
            String info = String.format("instance is not deployed, inst_id:{%s}", inst.getInstId());

            logger.info("{}", info);
            DeployLog.pubSuccessLog(logKey, info);

            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
            return true;
        }

        return false;
    }

    public static boolean getInstCmptName(String instID, StringBuilder cmptName, String logKey, ResultBean result) {
        String metaCmptName = MetaSvrGlobalRes.get().getCmptMeta().getInstCmptName(instID);
        if (StringUtils.isNull(metaCmptName)) {
            String err = String.format("inst_id: %s, %s", instID, FixDefs.ERR_METADATA_NOT_FOUND);
            DeployLog.pubFailLog(logKey, err);

            result.setRetCode(FixDefs.CODE_NOK);
            result.setRetInfo(err);
            return false;
        } else {
            cmptName.append(metaCmptName);
            return true;
        }
    }

    public static PaasSsh getSshById(String sshId, String logKey, ResultBean result) {
        PaasSsh ssh = MetaSvrGlobalRes.get().getCmptMeta().getSshById(sshId);
        if (ssh == null) {
            String info = String.format("ssh server for ssh_id:{%s} not found ......", sshId);

            if (result != null) {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(info);
            }

            if (!StringUtils.isNull(logKey)) {
                DeployLog.pubFailLog(logKey, info);
            }
            return null;
        }

        return ssh;
    }
    
    public static PaasSsh getSshById(String sshId) {
        return MetaSvrGlobalRes.get().getCmptMeta().getSshById(sshId);
    }

    public static boolean initSsh2(SSHExecutor ssh2, String logKey, ResultBean result) {
        return ssh2 != null && ssh2.login(logKey, result);
    }
    
    public static String getVersion(String servInstId, String containerInstId, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        // 优先级 instance.VERSION > container.VERSION > service.VERSION > deploy_file.VERSION
        int versionAttrId = 227;  // 227 -> 'VERSION'
        
        PaasInstAttr attr = cmptMeta.getInstAttr(instId, versionAttrId);
        if (attr != null && attr.getAttrValue() != null && !attr.getAttrValue().isEmpty()) {
            return attr.getAttrValue();
        }
        
        attr = cmptMeta.getInstAttr(containerInstId, versionAttrId);
        if (attr != null && attr.getAttrValue() != null && !attr.getAttrValue().isEmpty()) {
            return attr.getAttrValue();
        }
        
        PaasService serv = cmptMeta.getService(servInstId);
        return serv.getVersion();
    }
    
    public static String getServiceVersion(String servInstID, String instID) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        Vector<PaasTopology> servRelations = new Vector<PaasTopology>();
        meta.getInstRelations(servInstID, servRelations);
        String containerId = null;
        for (PaasTopology topo : servRelations) {
            String toeId = topo.getToe(servInstID);
            if (toeId == null || toeId.isEmpty())
                continue;
            
            Vector<PaasTopology> containerRelations = new Vector<PaasTopology>();
            meta.getInstRelations(toeId, containerRelations);
            
            for (PaasTopology subTopo : containerRelations) {
                String subInstId = subTopo.getToe(toeId);
                if (subInstId != null && subInstId.equals(instID)) {
                    containerId = toeId;
                    break;
                }
            }
            
            if (containerId != null)
                break;
        }

        // 版本更新获取需要更新的版本按优先级 service.VERSION > container.VERSION > instance.VERSION
        PaasService serv = meta.getService(servInstID);
        if (serv != null)
            return serv.getVersion();
        
        int versionAttrId = 227;  // 227 -> 'VERSION'
        PaasInstAttr attr = meta.getInstAttr(containerId, versionAttrId);
        if (attr != null && attr.getAttrValue() != null && !attr.getAttrValue().isEmpty()) {
            return attr.getAttrValue();
        }
        
        attr = meta.getInstAttr(instID, versionAttrId);
        return attr.getAttrValue();
    }
    
    public static boolean fetchFile(SSHExecutor ssh2, int fileId, String logKey, ResultBean result) {
        PaasDeployFile deployFile = DeployUtils.getDeployFile(fileId, logKey, result);
        if (deployFile == null)
            return false;
        
        int hostId = deployFile.getHostId();
        String srcFileName = deployFile.getFileName();
        String srcFileDir = deployFile.getFileDir();
        
        PaasDeployHost deployHost = MetaSvrGlobalRes.get().getCmptMeta().getDeployHost(hostId);
        String srcIp = deployHost.getIpAddress();
        String srcPort = deployHost.getSshPort();
        String srcUser = deployHost.getUserName();
        String srcPwd = deployHost.getUserPwd();
        
        String src_file = srcFileDir + srcFileName;
        String des_file = "./" + srcFileName;
        DeployLog.pubLog(logKey, "scp deploy file ......");
        if (!DeployUtils.scp(ssh2, srcUser, srcPwd, srcIp, srcPort, src_file, des_file, logKey, result))
            return false;
        
        return true;
    }
    
    public static String getVersionedFileName(int fileId, String version, String logKey, ResultBean result) {
        PaasDeployFile deployFile = DeployUtils.getDeployFile(fileId, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        return srcFileName.substring(0, idx);
    }
    
    public static boolean fetchAndExtractTGZDeployFile(SSHExecutor ssh2, int fileId, String subPath, String version,
                                                       String logKey, ResultBean result) {

        PaasDeployFile deployFile = DeployUtils.getDeployFile(fileId, logKey, result);
        if (deployFile == null)
            return false;

        int hostId = deployFile.getHostId();
        String srcFileName = deployFile.getFileName();
        String srcFileDir = deployFile.getFileDir();

        if (version == null || version.isEmpty())
            version = deployFile.getVersion();

        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        PaasDeployHost deployHost = MetaSvrGlobalRes.get().getCmptMeta().getDeployHost(hostId);
        String srcIp = deployHost.getIpAddress();
        String srcPort = deployHost.getSshPort();
        String srcUser = deployHost.getUserName();
        String srcPwd = deployHost.getUserPwd();

        String root_dir = String.format("%s/%s", FixDefs.PAAS_ROOT, subPath);

        DeployLog.pubLog(logKey, "create install dir ......");
        if (!DeployUtils.mkdir(ssh2, root_dir, logKey, result))
            return false;
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result))
            return false;

        String src_file = srcFileDir + srcFileName;
        String des_file = "./" + srcFileName;
        DeployLog.pubLog(logKey, "scp deploy file ......");
        if (!DeployUtils.scp(ssh2, srcUser, srcPwd, srcIp, srcPort, src_file, des_file, logKey, result))
            return false;

        // 防止文件没有下载下来
        if (!DeployUtils.isFileExist(ssh2, srcFileName, false, logKey, result)) {
            String info = String.format("scp file %s fail ......", srcFileName);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(info);
            return false;
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);

        DeployLog.pubLog(logKey, "unpack install tar file ......");
        if (!DeployUtils.tar(ssh2, FixDefs.TAR_ZXVF, srcFileName, oldName, logKey, result))
            return false;
        if (!DeployUtils.rm(ssh2, srcFileName, logKey, result))
            return false;

        return true;
    }

    public static boolean fetchAndExtractTGZDeployFiles(SSHExecutor ssh2, List<Integer> fileIdList, String subPath, String logKey, ResultBean result) {
        AtomicBoolean isSuccess = new AtomicBoolean(true);
        String rootDir = String.format("%s/%s", FixDefs.PAAS_ROOT, subPath);
        if (!DeployUtils.mkdir(ssh2, rootDir, logKey, result)) {
            isSuccess.set(false);
        }
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            isSuccess.set(false);
        }
        fileIdList.forEach(fileId -> {
            PaasDeployFile deployFile = DeployUtils.getDeployFile(fileId, logKey, result);
            if (deployFile == null) {
                isSuccess.set(false);
            }
            int hostId = deployFile.getHostId();
            String srcFileName = deployFile.getFileName();
            String version = deployFile.getVersion();
            if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
                srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
            }

            String srcFileDir = deployFile.getFileDir();
            PaasDeployHost deployHost = MetaSvrGlobalRes.get().getCmptMeta().getDeployHost(hostId);
            String srcIp = deployHost.getIpAddress();
            String srcPort = deployHost.getSshPort();
            String srcUser = deployHost.getUserName();
            String srcPwd = deployHost.getUserPwd();
            DeployLog.pubLog(logKey, "create install dir ......");
            String srcFile = srcFileDir + srcFileName;
            String desFile = "./" + srcFileName;
            DeployLog.pubLog(logKey, "scp deploy file ......");
            if (!DeployUtils.scp(ssh2, srcUser, srcPwd, srcIp, srcPort, srcFile, desFile, logKey, result)) {
                isSuccess.set(false);
            }
            
            // 防止文件没有下载下来
            if (!DeployUtils.isFileExist(ssh2, srcFileName, false, logKey, result)) {
                String info = String.format("scp file %s fail ......", srcFileName);
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(info);
                isSuccess.set(false);
            }
            
            int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
            String oldName = srcFileName.substring(0, idx);
            DeployLog.pubLog(logKey, "unpack install tar file ......");
            if (!DeployUtils.tar(ssh2, FixDefs.TAR_ZXVF, srcFileName, oldName, logKey, result)) {
                isSuccess.set(false);
            }
            if (!DeployUtils.rm(ssh2, srcFileName, logKey, result)) {
                isSuccess.set(false);
            }
        });
        return isSuccess.get();
    }

    public static boolean fetchAndExtractZipDeployFile(SSHExecutor ssh2, int fileId, String subPath, String oldName,
                                                       String version, String logKey, ResultBean result) {

        PaasDeployFile deployFile = DeployUtils.getDeployFile(fileId, logKey, result);
        if (deployFile == null)
            return false;

        int hostId = deployFile.getHostId();
        String srcFileName = deployFile.getFileName();
        String srcFileDir = deployFile.getFileDir();

        if (version == null || version.isEmpty())
            version = deployFile.getVersion();

        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        PaasDeployHost deployHost = MetaSvrGlobalRes.get().getCmptMeta().getDeployHost(hostId);
        String srcIp = deployHost.getIpAddress();
        String srcPort = deployHost.getSshPort();
        String srcUser = deployHost.getUserName();
        String srcPwd = deployHost.getUserPwd();

        String root_dir = String.format("%s/%s", FixDefs.PAAS_ROOT, subPath);

        DeployLog.pubLog(logKey, "create install dir ......");
        if (!DeployUtils.mkdir(ssh2, root_dir, logKey, result)) return false;
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) return false;

        String src_file = srcFileDir + srcFileName;
        String des_file = "./" + srcFileName;
        DeployLog.pubLog(logKey, "scp deploy file ......");
        if (!DeployUtils.scp(ssh2, srcUser, srcPwd, srcIp, srcPort, src_file, des_file, logKey, result)) return false;
        
        // 防止文件没有下载下来
        if (!DeployUtils.isFileExist(ssh2, srcFileName, false, logKey, result)) {
            String info = String.format("scp file %s fail ......", srcFileName);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(info);
            return false;
        }

        // int idx = srcFileName.indexOf(CONSTS.ZIP_SURFIX);
        // String oldName = srcFileName.substring(0, idx);

        DeployLog.pubLog(logKey, "unpack install tar file ......");
        if (!DeployUtils.rm(ssh2, oldName, logKey, result)) return false;
        if (!DeployUtils.unzip(ssh2, srcFileName, oldName, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "unzip " + srcFileName + " failed ......");
            return false;
        }
        if (!DeployUtils.rm(ssh2, srcFileName, logKey, result)) return false;

        return true;
    }

    public static PaasDeployFile getDeployFile(int fileId, String logKey, ResultBean result) {
        PaasDeployFile deployFile = MetaSvrGlobalRes.get().getCmptMeta().getDeployFile(fileId);
        if (deployFile == null) {
            String info = String.format("deploy file id: %d not found ......", fileId);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(info);

            DeployLog.pubFailLog(logKey, info);
        }

        return deployFile;
    }

    public static String getEtcdLongAddr(JsonArray etcdNodeArr) {
        StringBuilder sb = new StringBuilder("");
        int maxIdx = etcdNodeArr.size() - 1;
        for (int i = 0; i <= maxIdx; ++i) {
            JsonObject etcdNode = etcdNodeArr.getJsonObject(i);
            String sshId = etcdNode.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String etcdNodeIp = ssh.getServerIp();
            String clientUrlsPort = etcdNode.getString(FixHeader.HEADER_CLIENT_URLS_PORT);
            String addr = String.format("    - \"http\\:\\/\\/%s\\:%s\"", etcdNodeIp, clientUrlsPort);
            
            sb.append(addr);
            if (i < maxIdx) {
                sb.append("\\n");
            }
        }
        
        return sb.toString();
    }

    public static String getEtcdShortAddr(JsonArray etcdNodeArr) {
        StringBuilder sb = new StringBuilder("");
        int maxIdx = etcdNodeArr.size() - 1;
        for (int i = 0; i <= maxIdx; ++i) {
            JsonObject etcdNode = etcdNodeArr.getJsonObject(i);
            String sshId = etcdNode.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String etcdNodeIp = ssh.getServerIp();
            String clientUrlsPort = etcdNode.getString(FixHeader.HEADER_CLIENT_URLS_PORT);
            String addr = String.format("- %s:%s", etcdNodeIp, clientUrlsPort);
            
            sb.append(addr);
            if (i < maxIdx) {
                sb.append(CONSTS.LINE_END);
            }
        }
        
        return sb.toString();
    }

    public static String getEtcdFullAddr(JsonArray etcdNodeArr) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < etcdNodeArr.size(); ++i) {
            JsonObject etcdNode = etcdNodeArr.getJsonObject(i);
            String sshId = etcdNode.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String etcdNodeIp = ssh.getServerIp();
            String peerUrlsPort = etcdNode.getString(FixHeader.HEADER_PEER_URLS_PORT);
            String etcdInstId = etcdNode.getString(FixHeader.HEADER_INST_ID);
            String addr = String.format("%s=http://%s:%s", etcdInstId, etcdNodeIp, peerUrlsPort);
            
            if (i > 0) {
                sb.append(CONSTS.PATH_COMMA);
            }
            sb.append(addr);
        }
        
        return sb.toString();
    }

    public static boolean getRedisClusterNode(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            // 先把先前执行完未read缓冲区清空
            ssh2.consumeSurplusBuf();

            String context = ssh2.generalCommand(cmd);


            // 截取中间redis cluster info部分
            /*
             *
             *  ./bin/redis-cli -h 172.20.0.41 -p 10001 -c -
             *  -no-auth-warning cluster nodes
             *  adc69919eb866be90d1898a3987d6ff5f9ba5aa8 172.20.0.41:10003@20003 master - 0 1614130917547 3 connected 10923-16383
             *  2d94bc8ca9e77eecfd257934868598a05a1495af 172.20.0.41:10001@20001 myself,master - 0 1614130916000 1 connected 0-5460
             *  32b09cca44af480467299a1778dc8b5ab442ee69 172.20.0.41:10004@20004 slave 2b5d3168fe1f445b959324b33839210243f868fc 0 1614130919551 2 connected
             *  ee14abef1f2a8041a21ad0b6a67430e637f98a3e 172.20.0.41:10006@20006 slave 2d94bc8ca9e77eecfd257934868598a05a1495af 0 1614130917000 1 connected
             *  2b5d3168fe1f445b959324b33839210243f868fc 172.20.0.41:10002@20002 master - 0 1614130918549 2 connected 5461-10922
             *  35a71a98934e78dd4d3e2cbfddda52529f50acf7 172.20.0.41:10005@20005 slave adc69919eb866be90d1898a3987d6ff5f9ba5aa8 0 1614130916545 3 connected
             *  [sms@host-172-20-0-41 redis_10001]$
             *
             */

            int first = context.indexOf("@", 0);
            if (first == -1) return false;

            int start = context.lastIndexOf("\n", first);
            int end = context.lastIndexOf("\n");

            String subStr = context.substring(start + 1, end - 1);
            result.setRetInfo(subStr);

        } catch (Exception e) {
            res = false;

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static boolean removeFromRedisCluster(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            ssh2.consumeSurplusBuf();

            res = ssh2.removeFromRedisCluster(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean migrateRedisClusterSlot(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            ssh2.consumeSurplusBuf();

            res = ssh2.migrateRedisClusterSlot(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean redisSlaveof(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            ssh2.consumeSurplusBuf();

            res = ssh2.redisSlaveof(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean mkdir(SSHExecutor ssh2, String dir, String logKey, ResultBean result) {
        String cmd = String.format("%s -p %s", SSHExecutor.CMD_MKDIR, dir);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }

    public static String pwd(SSHExecutor ssh2, String logKey, ResultBean result) {
        String sshResult = "";
        try {
            sshResult = ssh2.pwd();
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return sshResult;

    }

    public static boolean cd(SSHExecutor ssh2, String dir, String logKey, ResultBean result) {
        String cmd = String.format("%s %s", SSHExecutor.CMD_CD, dir);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }

    public static boolean cdOnFailClose(SSHExecutor ssh2, String dir, String logKey, ResultBean result) {
        String cmd = String.format("%s %s", SSHExecutor.CMD_CD, dir);
        boolean res = execSimpleCmd(ssh2, cmd, logKey, result);
        if (!res)
            ssh2.close();
        
        return res;
    }
    
    public static boolean tar(SSHExecutor ssh2, String tarParams, String srcFileName, String desFileName, String logKey,
                              ResultBean result) {
        String cmd = String.format("%s %s %s", SSHExecutor.CMD_TAR, tarParams, srcFileName);
        if (!execSimpleCmd(ssh2, cmd, logKey, result))
            return false;

        return isFileExist(ssh2, desFileName, true, logKey, result);
    }

    public static boolean createValidationTable(SSHExecutor ssh2, String sqlCmd, String logKey, ResultBean result) {
        boolean res = true;

        try {
            res = ssh2.createValidationTable(sqlCmd, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            res = false;

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
            
            ssh2.close();
        }
        
        return res;
    }
    
    public static boolean unzip(SSHExecutor ssh2, String srcFileName, String desFileName, String logKey,
                                ResultBean result) {
        boolean res = true;
        try {
            String cmd = String.format("%s -o %s", SSHExecutor.CMD_UNZIP, srcFileName);
            res = ssh2.unzip(cmd, logKey, result);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        if (!res) {
            return res;
        }
        return isFileExist(ssh2, desFileName, true, logKey, result);
    }

    public static boolean rm(SSHExecutor ssh2, String fileName, String logKey, ResultBean result) {
        boolean res = true;
        try {
            ssh2.rm(fileName, true, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            res = false;

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }
    
    public static boolean rmOnFailClose(SSHExecutor ssh2, String fileName, String logKey, ResultBean result) {
        boolean res = rm(ssh2, fileName, logKey, result);
        if (!res)
            ssh2.close();
        
        return res;
    }

    public static boolean isFileExist(SSHExecutor ssh2, String fileName, boolean isDir, String logKey, ResultBean result) {
        boolean res = false;
        try {
            res = ssh2.isFileExist(fileName, isDir, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static boolean scp(SSHExecutor ssh2, String srcUser, String srcPwd, String srcIp, String srcPort,
                              String srcFile, String desFile, String logKey, ResultBean result) {

        boolean res = false;
        try {
            res = ssh2.scp(srcUser, srcPwd, srcIp, srcFile, desFile, srcPort, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static boolean mv(SSHExecutor ssh2, String newName, String oldName, String logKey, ResultBean result) {
        String cmd = String.format("%s %s %s", SSHExecutor.CMD_MV, oldName, newName);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }
    
    public static boolean mvOnFailClose(SSHExecutor ssh2, String newName, String oldName, String logKey, ResultBean result) {
        String cmd = String.format("%s %s %s", SSHExecutor.CMD_MV, oldName, newName);
        if (execSimpleCmd(ssh2, cmd, logKey, result)) {
            return true;
        } else {
            ssh2.close();
            return false;
        }
    }

    public static boolean sed(SSHExecutor ssh2, String oldValue, String newValue, String file, String logKey, ResultBean result) {
        String cmd = String.format("%s -i 's/%s/%s/g' %s", SSHExecutor.CMD_SED, oldValue, newValue, file);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }
    
    public static boolean sedOnFailClose(SSHExecutor ssh2, String oldValue, String newValue, String file, String logKey, ResultBean result) {
        String cmd = String.format("%s -i 's/%s/%s/g' %s", SSHExecutor.CMD_SED, oldValue, newValue, file);
        if (execSimpleCmd(ssh2, cmd, logKey, result)) {
            return true;
        } else {
            ssh2.close();
            return false;
        }
    }
    
    public static boolean appendMultiLine(SSHExecutor ssh2, String oldValue, String newValue, String file, String logKey, ResultBean result) {
        String cmd = String.format("%s -i '/%s/a\\%s' %s", SSHExecutor.CMD_SED, oldValue, newValue, file);
        if (!execSimpleCmd(ssh2, cmd, logKey, result))
            return false;
        
        // cmd = String.format("%s -i 's/%s/%s/g' %s", SSHExecutor.CMD_SED, oldValue, "", file);
        // sed '/System/d' sed-demo.txt
        cmd = String.format("%s -i '/%s/d' %s", SSHExecutor.CMD_SED, oldValue, file);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }

    public static boolean addLine(SSHExecutor ssh2, String context, String confFile, String logKey, ResultBean result) {
        // if (!isFileExist(ssh2, confFile, false, logKey, result)) return false;

        if (!execSimpleCmd(ssh2, SSHExecutor.CMD_SETH_PLUS, logKey, result)) return false;

        String cmd = String.format("%s -e \"%s\">>%s\n", SSHExecutor.CMD_ECHO, context, confFile);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }

    public static boolean createShell(SSHExecutor ssh2, String fileName, String shell, String logKey, ResultBean result) {
        if (isFileExist(ssh2, fileName, false, logKey, result))
            rm(ssh2, fileName, logKey, result);

        if (!execSimpleCmd(ssh2, SSHExecutor.CMD_SETH_PLUS, logKey, result)) return false;

        String cmd = String.format("%s -e \"%s\\n\">>%s", SSHExecutor.CMD_ECHO, FixDefs.SHELL_MACRO, fileName);
        if (!execSimpleCmd(ssh2, cmd, logKey, result)) return false;

        cmd = String.format("%s -e \"%s\">>%s", SSHExecutor.CMD_ECHO, shell, fileName);
        if (!execSimpleCmd(ssh2, cmd, logKey, result)) return false;

        return chmod(ssh2, fileName, "+x", logKey, result);
    }

    public static boolean createFile(SSHExecutor ssh2, String fileName, String fileContent, String logKey, ResultBean result) {
        if (isFileExist(ssh2, fileName, false, logKey, result)) {
            rm(ssh2, fileName, logKey, result);
        }
        if (!execSimpleCmd(ssh2, SSHExecutor.CMD_SETH_PLUS, logKey, result)) {
            return false;
        }
        int idx = fileName.lastIndexOf("/");
        String cmd = String.format("%s %s", SSHExecutor.CMD_MKDIR, fileName.substring(0, idx));
        if (!execSimpleCmd(ssh2, cmd, logKey, result)) {
            return false;
        }
        cmd = String.format("%s -e \"%s\">>%s", SSHExecutor.CMD_ECHO, fileContent, fileName);
        if (!execSimpleCmd(ssh2, cmd, logKey, result)) {
            return false;
        }
        return chmod(ssh2, fileName, "+x", logKey, result);
    }

    public static boolean chmod(SSHExecutor ssh2, String fileName, String mod, String logKey, ResultBean result) {
        String cmd = String.format("%s %s %s", SSHExecutor.CMD_CHMOD, mod, fileName);
        return execSimpleCmd(ssh2, cmd, logKey, result);
    }

    public static boolean dos2unix(SSHExecutor ssh2, String fileName, String logKey, ResultBean result) {
        boolean res = true;
        try {
            String cmd = String.format("%s %s", SSHExecutor.CMD_DOS2UNIX, fileName);
            res = ssh2.dos2unix(cmd, logKey, result);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean checkPortUpPredeploy(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
            String logKey, ResultBean result) {
        
        ssh2.consumeSurplusBuf();
        boolean using = isPortUsed(ssh2, port, logKey, result);
        if (using) {
            String info = String.format("port: %s is in using", port);
            DeployLog.pubFailLog(logKey, info);
        }
        return using;
    }
    
    public static boolean checkPortUpPredeployOnClose(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
            String logKey, ResultBean result) {
        
        ssh2.consumeSurplusBuf();
        boolean using = isPortUsed(ssh2, port, logKey, result);
        if (using) {
            String info = String.format("port: %s is in using", port);
            DeployLog.pubFailLog(logKey, info);
            ssh2.close();
        }
        return using;
    }

    public static boolean checkPortUp(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
            String logKey, ResultBean result) {
        
        ssh2.consumeSurplusBuf();
        
        boolean isUsed = false;
        for (int i = 0; i < FixDefs.CHECK_PORT_RETRY; ++i) {
            if ((isUsed = isPortUsed(ssh2, port, logKey, result))) {
                break;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (isUsed) {
            String info = String.format("deploy %s success, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, servIp,
                    port);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("deploy %s fail, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, servIp, port);
            DeployLog.pubFailLog(logKey, info);
        }

        return isUsed;
    }

    public static boolean checkPortUpOnFailClose(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
            String logKey, ResultBean result) {

        boolean res = checkPortUp(ssh2, cmpt, instId, servIp, port, logKey, result);
        if (!res)
            ssh2.close();

        return res;
    }

    public static boolean checkPortDown(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
                                        String logKey, ResultBean result) {
        
        ssh2.consumeSurplusBuf();
        
        boolean isUsed = true;
        for (int i = 0; i < FixDefs.CHECK_PORT_RETRY; ++i) {
            if (!(isUsed = isPortUsed(ssh2, port, logKey, result))) {
                break;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (isUsed) {
            String info = String.format("shutdown %s fail, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, servIp, port);
            DeployLog.pubFailLog(logKey, info);
        } else {
            String info = String.format("shutdown %s success, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, servIp, port);
            DeployLog.pubSuccessLog(logKey, info);
        }

        return !isUsed;
    }

    public static boolean checkPortDownOnFailClose(SSHExecutor ssh2, String cmpt, String instId, String servIp, String port,
            String logKey, ResultBean result) {
    
        boolean res = checkPortDown(ssh2, cmpt, instId, servIp, port, logKey, result);
        if (!res)
            ssh2.close();
        
        return res;
    }
    
    public static boolean isPortUsed(SSHExecutor ssh2, String port, String logKey, ResultBean result) {
        boolean res = false;
        try {
            res = ssh2.isPortUsed(port, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }
    
    public static boolean isProcExist(SSHExecutor ssh2, String identify, String logKey, ResultBean result) {
        boolean res = false;
        try {
            res = ssh2.isProcExist(identify, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }
    
    public static boolean initRedisCluster(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = false;
        try {
            res = ssh2.initRedisCluster(cmd, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static boolean joinRedisCluster(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = false;
        try {
            res = ssh2.joinRedisCluster(cmd, logKey);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static String getRedisNodeId(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        String res = null;
        try {
            res = ssh2.generalCommand(cmd);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return res;
    }

    public static boolean execSimpleCmd(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean sshResult = true;
        try {
            sshResult = ssh2.execSimpleCmd(cmd, logKey, SSHExecutor.PRINT_LOG);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return sshResult;
    }
    
    public static boolean execSimpleCmdOnFailClose(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = execSimpleCmd(ssh2, cmd, logKey, result);
        if (!res)
            ssh2.close();
        
        return res;
    }

    public static String execGeneralCmd(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        String sshResult = "";
        try {
            sshResult = ssh2.generalCommand(cmd);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return sshResult;
    }

    public static boolean isExistedJdk(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            res = ssh2.isExistedJdk(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static JsonObject getSpecifiedItem(JsonArray jsonArr, String instID) {
        JsonObject result = null;
        for (int i = 0; i < jsonArr.size(); ++i) {
            JsonObject item = jsonArr.getJsonObject(i);
            String id = item.getString(FixHeader.HEADER_INST_ID);
            if (id.equals(instID)) {
                result = item;
                break;
            }
        }
        return result;
    }
    
    public static JsonObject getSpecifiedRocketMQBroker(JsonArray vbrokerArr, String instID) {
        JsonObject result = null;
        for (int i = 0; i < vbrokerArr.size(); ++i) {
            JsonObject vbroker = vbrokerArr.getJsonObject(i);
            JsonArray brokerArr = vbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            
            for (int j = 0; j< brokerArr.size(); j++) {
                JsonObject broker = brokerArr.getJsonObject(j);
                String brokerInstId = broker.getString(FixHeader.HEADER_INST_ID);
                if (brokerInstId.equals(instID)) {
                    result = broker;
                    break;
                }
            }
            
            if (result != null) {
                break;
            }
        }
        
        return result;
    }
    
    public static String getSpecifiedVBrokerId(JsonArray vbrokerArr, String instID) {
        String result = null;
        for (int i = 0; i < vbrokerArr.size(); ++i) {
            JsonObject vbroker = vbrokerArr.getJsonObject(i);
            JsonArray brokerArr = vbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            
            for (int j = 0; j< brokerArr.size(); j++) {
                JsonObject broker = brokerArr.getJsonObject(j);
                String brokerInstId = broker.getString(FixHeader.HEADER_INST_ID);
                if (brokerInstId.equals(instID)) {
                    result = vbroker.getString(FixHeader.HEADER_INST_ID);
                    break;
                }
            }
            
            if (result != null) {
                break;
            }
        }
        
        return result;
    }
    
    public static JsonArray getSpecifiedBrokerArr(JsonArray vbrokerArr, String instID) {
        JsonArray result = null;
        for (int i = 0; i < vbrokerArr.size(); ++i) {
            JsonObject vbroker = vbrokerArr.getJsonObject(i);
            JsonArray brokerArr = vbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            
            for (int j = 0; j< brokerArr.size(); j++) {
                JsonObject broker = brokerArr.getJsonObject(j);
                String brokerInstId = broker.getString(FixHeader.HEADER_INST_ID);
                if (brokerInstId.equals(instID)) {
                    result = brokerArr;
                    break;
                }
            }
            
            if (result != null) {
                break;
            }
        }
        
        return result;
    }
    
    public static JsonObject getSpecifiedClickHouseItem(JsonArray jsonArr, String instID) {
        JsonObject result = null;
        for (int i = 0; i < jsonArr.size(); ++i) {
            JsonObject replicas = jsonArr.getJsonObject(i);
            JsonArray clickHouseArr = replicas.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);
                String id = clickhouse.getString(FixHeader.HEADER_INST_ID);
                if (id.equals(instID)) {
                    result = clickhouse;
                    break;
                }
            }
            
            if (result != null) {
                break;
            }
        }
        return result;
    }
    
    public static String getSpecifiedClickHouseParentID(JsonArray jsonArr, String instID) {
        String result = null;
        for (int i = 0; i < jsonArr.size(); ++i) {
            JsonObject replicas = jsonArr.getJsonObject(i);
            JsonArray clickHouseArr = replicas.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);
                String id = clickhouse.getString(FixHeader.HEADER_INST_ID);
                if (id.equals(instID)) {
                    result = replicas.getString(FixHeader.HEADER_INST_ID);
                    break;
                }
            }
            
            if (result != null) {
                break;
            }
        }
        return result;
    }

    public static JsonArray pdctlGetStore(SSHExecutor ssh2, String ip, String port, String logKey, ResultBean result) {
        JsonObject stores = null;
        try {
            String cmd = String.format("./bin/pd-ctl -u http://%s:%s  store", ip, port);
            String context = ssh2.generalCommand(cmd);
            int start = context.indexOf(cmd);
            start = context.indexOf("\n", start);
            start += "\n".length();
            int end = context.lastIndexOf("\n");
            String pdInfo = context.substring(start, end);
            stores = new JsonObject(pdInfo);
        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }
        return stores.getJsonArray("stores");
    }

    public static Integer getStoreId(SSHExecutor ssh2, String pdIp, String pdPort, String ip, String port,
                                     String logKey, ResultBean result) {
        JsonArray arr = pdctlGetStore(ssh2, pdIp, pdPort, logKey, result);
        int res = 0;
        for (Object obj : arr) {
            JsonObject json = (JsonObject) obj;
            JsonObject store = json.getJsonObject("store");
            String address = store.getString("address");
            if (address.equals(ip + ":" + port)) {
                return store.getInteger("id");
            }
        }
        return res;
    }

    public static boolean pdctlDeleteTikvStore(SSHExecutor ssh2, String pdIp, String pdPort,
                                               int id, String logKey, ResultBean result) {
        String context = "";
        try {
            String cmd = String.format("./bin/pd-ctl -u http://%s:%s store delete %d \n", pdIp, pdPort, id);
            context = ssh2.generalCommand(cmd);

        } catch (SSHException e) {
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return context.indexOf(CONSTS.PD_DELETE_STORE_SUCC) != -1 ? Boolean.TRUE : Boolean.FALSE;
    }

    public static boolean deployZKNode(JsonObject zk, int idx, String version, String zkAddrList, String logKey, String magicKey, ResultBean result) {
        String sshId = zk.getString(FixHeader.HEADER_SSH_ID);
        String clientPort = zk.getString(FixHeader.HEADER_CLIENT_PORT);
        String adminPort = zk.getString(FixHeader.HEADER_ADMIN_PORT);
        String instId = zk.getString(FixHeader.HEADER_INST_ID);
        
        String clientPort1 = zk.getString(FixHeader.HEADER_ZK_CLIENT_PORT1);
        String clientPort2 = zk.getString(FixHeader.HEADER_ZK_CLIENT_PORT2);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy zookeeper: %s:%s, instId:%s", servIp, adminPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "zookeeper", instId, servIp, clientPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("zookeeper.clientPort端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "zookeeper", instId, servIp, adminPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("zookeeper.adminServerPort端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "zookeeper", instId, servIp, clientPort1, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("zookeeper.ClientPort端口1 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "zookeeper", instId, servIp, clientPort2, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("zookeeper.ClientPort端口2 is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.ZK_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, version, logKey, result))
            return false;
        //修改文件名
        PaasDeployFile zkDeployFile = DeployUtils.getDeployFile(FixDefs.ZK_FILE_ID, logKey, result);
        String srcFileName = zkDeployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = zkDeployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + adminPort;
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
        // dataDir=%DATA_DIR%
        // dataLogDir=%LOG_DIR%
        // admin.serverPort=%ADMIN_PORT%
        DeployLog.pubLog(logKey, "modify start.sh and stop.sh env params ......");
        
        String pwd = DeployUtils.pwd(ssh2, logKey, result);
        String dataDir = String.format("%s/%s", pwd, "data");
        String logDir = String.format("%s/%s/%s", pwd, "data", "log");
        
        dataDir = dataDir.replaceAll("/", "\\\\/");
        logDir = logDir.replaceAll("/", "\\\\/");
        
        String configFile = "./conf/zoo.cfg";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DATA_DIR, dataDir, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LOG_DIR, logDir, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADMIN_PORT, adminPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.addLine(ssh2, zkAddrList, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLIENT_PORT, clientPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLIENT_ADDRESS, servIp, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String myIdFile = "data/myid";
        String context = String.format("%d", idx);
        if (!DeployUtils.addLine(ssh2, context, myIdFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start zookeeper ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "zookeeper", instId, servIp, adminPort, logKey, result)) {
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

    public static boolean undeployZookeeper(JsonObject zk, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = zk.getString(FixHeader.HEADER_SSH_ID);
        String adminPort = zk.getString(FixHeader.HEADER_ADMIN_PORT);
        String instId = zk.getString(FixHeader.HEADER_INST_ID);

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy zookeeper, inst_id:%s, serv_ip:%s, admin_port:%s", instId, servIp, adminPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile zkDeployFile = DeployUtils.getDeployFile(FixDefs.ZK_FILE_ID, logKey, result);
        String srcFileName = zkDeployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = zkDeployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = String.format("%s_%s", oldName, adminPort);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop zookeeper ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
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
        
        if (!DeployUtils.checkPortDown(ssh2, "zookeeper", instId, servIp, adminPort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }
    
    public static boolean deployGrafana(JsonObject grafana, String version, String logKey, String magicKey, ResultBean result) {
        String instId = grafana.getString(FixHeader.HEADER_INST_ID);
        String sshId = grafana.getString(FixHeader.HEADER_SSH_ID);
        String httpPort = grafana.getString(FixHeader.HEADER_HTTP_PORT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy grafana: %s:%s, instId:%s", servIp, httpPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "grafana", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("grafana.httpPort is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.GRAFANA_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.GRAFANA_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + httpPort;
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
        
        // conf/defaults.ini
        // http_addr = %HTTP_ADDR%
        // http_port = %HTTP_PORT%
        // domain = %DOMAIN%
        String confFile = String.format("conf/defaults.ini");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HTTP_ADDR, servIp, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HTTP_PORT, httpPort, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DOMAIN, servIp, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop.sh
        // %GRAFANA_DIR%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_GRAFANA_DIR, newName, FixDefs.STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start grafana ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "grafana", instId, servIp, httpPort, logKey, result)) {
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
    
    public static boolean undeployGrafana(JsonObject grafana, String version, String logKey, String magicKey,
            ResultBean result) {

        String instId = grafana.getString(FixHeader.HEADER_INST_ID);
        String sshId = grafana.getString(FixHeader.HEADER_SSH_ID);
        String httpPort = grafana.getString(FixHeader.HEADER_HTTP_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy grafana, inst_id:%s, serv_ip:%s, http_port:%s", instId, servIp, httpPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.GRAFANA_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + httpPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop grafana ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
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

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }
    
    public static boolean deployEtcdNode(JsonObject etcdJson, String etcdNodes, String etcdContainerInstId, String logKey, String magicKey, ResultBean result) {
        String instId = etcdJson.getString(FixHeader.HEADER_INST_ID);
        String sshId = etcdJson.getString(FixHeader.HEADER_SSH_ID);
        String clientUrlsPort = etcdJson.getString(FixHeader.HEADER_CLIENT_URLS_PORT);
        String peerUrlsPort = etcdJson.getString(FixHeader.HEADER_PEER_URLS_PORT);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) {
            return false;
        }
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        String info = String.format("start deploy etcd, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, clientUrlsPort);
        DeployLog.pubLog(logKey, info);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            return true;
        }
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) {
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "etcd", instId, servIp, clientUrlsPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("etcd.clientUrlsPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "etcd", instId, servIp, peerUrlsPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("etcd.peerUrlsPort is in use");
            return false;
        }
        
        // SERVERLESS_ETCD_FILE_ID -> 'etcd-3.4.14.tar.gz'
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.SERVERLESS_ETCD_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, "", logKey, result)) {
            return false;
        }
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.SERVERLESS_ETCD_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        String version     = deployFile.getVersion();
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);
        String newName = "etcd_" + clientUrlsPort;
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
        
        DeployLog.pubLog(logKey, "replace start and stop shell variants ......");
        // start.sh
        // --name %NAME% \
        // --listen-client-urls http://%LISTEN_CLIENT_URLS% \
        // --listen-peer-urls http://%LISTEN_PEER_URLS% \
        // --advertise-client-urls http://%ADVERTISE_CLIENT_URLS% \
        // --initial-advertise-peer-urls http://%ADVERTISE_PEER_URLS% \
        // --initial-cluster %CLUSTER_NODES% \
        // --initial-cluster-token %CLUSTER_TOKEN% \
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_NAME, instId, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String listenClntUrls = String.format("%s:%s", servIp, clientUrlsPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_CLIENT_URLS, listenClntUrls, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String listenPeerUrls = String.format("%s:%s", servIp, peerUrlsPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_PEER_URLS, listenPeerUrls, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String advertiseClntUrls = String.format("%s:%s", servIp, clientUrlsPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADVERTISE_CLIENT_URLS, advertiseClntUrls, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String advertisePeerUrls = String.format("%s:%s", servIp, peerUrlsPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADVERTISE_PEER_URLS, advertisePeerUrls, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String clusterNodes = etcdNodes.replaceAll("/", "\\\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NODES, clusterNodes, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String clusterToken = DigestUtils.md5Hex(etcdContainerInstId);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_TOKEN, clusterToken, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start etcd ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        DeployLog.pubLog(logKey, "checkPortUp etcd ......");
        if (!DeployUtils.checkPortUp(ssh2, "etcd", instId, servIp, clientUrlsPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        DeployLog.pubLog(logKey, "updateInstanceDeployFlag etcd ......");
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        ssh2.close();
        DeployLog.pubLog(logKey, "end etcd ......");
        return true;
    }
    
    public static boolean undeployEtcdNode(JsonObject etcdJson, String logKey, String magicKey, ResultBean result) {
        String instId = etcdJson.getString(FixHeader.HEADER_INST_ID);
        String sshId = etcdJson.getString(FixHeader.HEADER_SSH_ID);
        String clientUrlsPort = etcdJson.getString(FixHeader.HEADER_CLIENT_URLS_PORT);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) {
            return false;
        }
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        String info = String.format("start undeploy etcd, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, clientUrlsPort);
        DeployLog.pubLog(logKey, info);
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            return true;
        }
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) {
            return false;
        }
        String etcdDir = String.format("etcd_%s", clientUrlsPort);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, etcdDir);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop etcd ......");
        String shell = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, shell, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.checkPortDown(ssh2, "etcd", instId, servIp, clientUrlsPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, etcdDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        ssh2.close();
        return true;
    }
    
    public static boolean updateSerivceDeployFlag(String servInstID, String flag, String logKey, String magicKey, ResultBean result) {
        // 更新服务和实例部署标志: is_deployed = 1
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, flag, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, flag, result, magicKey)) {
            return false;
        }

        String action = flag.equals(FixDefs.STR_TRUE) ? "deploy" : "undeploy";
        String info = String.format("service inst_id:%s, %s success ......", servInstID, action);
        DeployLog.pubSuccessLog(logKey, info);
        
        return true;
    }
    
    public static boolean resetDBPwd(JsonObject jsonTidbServer, String logKey, ResultBean result) {
        String sshId = jsonTidbServer.getString(FixHeader.HEADER_SSH_ID);
        String port = jsonTidbServer.getString(FixHeader.HEADER_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        String servIp = ssh.getServerIp();
        
        return setTiDBPwdProc(servIp, port, logKey, result);
    }
    
    private static boolean setTiDBPwdProc(String ip, String port, String logKey, ResultBean result) {
        boolean ret = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String DBAddress = String.format("jdbc:mysql://%s:%s?user=root&useUnicode=true&characterEncoding=UTF8&useSSL=false", ip, port);
            conn = DriverManager.getConnection(DBAddress);
            
            stmt = conn.prepareStatement("SET PASSWORD FOR 'root'@'%' = ?");
            stmt.setString(1, FixDefs.DEFAULT_TIDB_ROOT_PASSWD);
            stmt.execute();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_CONNECT_TIDB_SERVER_ERROR + e.getMessage());
            ret = false;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        
        return ret;
    }
    
    public static boolean isPreEmbadded(String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstAttr paasInstAttr = cmptMeta.getInstAttr(instId, 320); // 320, 'PRE_EMBEDDED'
        if (paasInstAttr == null)
            return false;
        
        return paasInstAttr.getAttrValue().equals(FixDefs.S_TRUE);
    }
    
    public static void sleepMilliSeconds(long milliSeconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliSeconds);
        } catch (InterruptedException e) {
            ;
        }
    }
    
    public static void sleepSeconds(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            ;
        }
    }
    
    public static JsonObject getSelfNode(JsonArray nodeArr, String instID) {
        int size = nodeArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject currNode = nodeArr.getJsonObject(i);
            String currID = currNode.getString(FixHeader.HEADER_INST_ID);
            if (currID.equals(instID)) {
                return currNode;
            }
        }
        
        return null;
    }
    
    public static String getZKAddress(JsonArray zkArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        StringBuilder buff = new StringBuilder();
        int size = zkArr.size();
        for (int idx = 0; idx < size; ++idx) {
            JsonObject item = zkArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String clientPort1 = item.getString(FixHeader.HEADER_ZK_CLIENT_PORT1);
            String clientPort2 = item.getString(FixHeader.HEADER_ZK_CLIENT_PORT2);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            // server.1=host1:2888:3888
            // server.2=host2:2888:3888
            // server.3=host3:2888:3888
            String line = String.format("server.%d=%s:%s:%s", (idx + 1), servIP, clientPort1, clientPort2);
            if (idx > 0)
                buff.append("\n");
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getZKShortAddress(JsonArray zkArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        StringBuilder buff = new StringBuilder();
        int size = zkArr.size();
        for (int idx = 0; idx < size; ++idx) {
            JsonObject item = zkArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String clientPort = item.getString(FixHeader.HEADER_CLIENT_PORT);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, clientPort);
            if (idx > 0)
                buff.append(",");
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static boolean deployPrometheus(JsonObject prometheus, String pulsarClusterName, String brokers,
            String bookies, String zks, String version, String logKey, String magicKey, ResultBean result) {

        String instId = prometheus.getString(FixHeader.HEADER_INST_ID);
        String sshId = prometheus.getString(FixHeader.HEADER_SSH_ID);
        String prometheusPort = prometheus.getString(FixHeader.HEADER_PROMETHEUS_PORT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy prometheus: %s:%s, instId:%s", servIp, prometheusPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "prometheus", instId, servIp, prometheusPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("prometheus.prometheusPort is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.PROMETHEUS_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PROMETHEUS_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + prometheusPort;
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
        
        // start.sh stop.sh %LISTEN_ADDRESS%
        String prometheusAddr = String.format("%s:%s", servIp, prometheusPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_ADDRESS, prometheusAddr, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_ADDRESS, prometheusAddr, FixDefs.STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // scp prometheus_pulsar.yml
        if (!DeployUtils.fetchFile(ssh2, FixDefs.PROMETHEUS_PULSAR_YML_FILE_ID, logKey, result)) {
            DeployLog.pubFailLog(logKey, "scp prometheus_pulsar.yml fail ......");
            return false;
        }
        
        // cluster: %CLUSTER_NAME%
        // %PULSAR_BROKERS%
        // %PULSAR_BOOKIES%
        // %PULSAR_ZOOKEEPERS%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NAME, pulsarClusterName, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_BROKERS, brokers, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_BOOKIES, bookies, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_ZOOKEEPERS, zks, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, FixDefs.PROMETHEUS_YML, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start prometheus ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "prometheus", instId, servIp, prometheusPort, logKey, result)) {
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
    
    public static boolean undeployPrometheus(JsonObject prometheus, String version, String logKey, String magicKey,
            ResultBean result) {

        String instId = prometheus.getString(FixHeader.HEADER_INST_ID);
        String sshId = prometheus.getString(FixHeader.HEADER_SSH_ID);
        String prometheusPort = prometheus.getString(FixHeader.HEADER_PROMETHEUS_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy prometheus, inst_id:%s, serv_ip:%s, prometheus_port:%s", instId, servIp, prometheusPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PROMETHEUS_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + prometheusPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop prometheus ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
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

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }

}
