package com.zzstack.paas.underlying.metasvr.autodeploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SSHExecutor;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.exception.SSHException;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class DeployerMarshell {
    
    private static Logger logger = LoggerFactory.getLogger(DeployerMarshell.class);

    public static void deployService(String instID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        try {
            PaasService service = DeployUtils.getService(instID, logKey, result);
            if (service == null)
                return;

            if (DeployUtils.isServiceDeployed(service, logKey, result))
                return;

            boolean deployResult = false;
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(service.getServType(), logKey);
            if (serviceDeployer != null) {
                deployResult = serviceDeployer.deployService(instID, deployFlag, logKey, magicKey, result);
            } else {
                String err = String.format("service type not found, service_id:%s, service_type:%s", instID,
                        service.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
            }

            if (deployResult) {
                logger.info("service deploy success ......");
            } else {
                result.setRetCode(CONSTS.REVOKE_NOK);
                if (result.getRetInfo() == null)
                    result.setRetInfo(CONSTS.ERR_INTERNAL);
                logger.error("service deploy fail ......");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            DeployLog.pubFailLog(logKey, "deploy service fail ......");
            DeployLog.pubFailLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INTERNAL);
        }
    }

    public static void undeployService(String instID, boolean force, String logKey, String magicKey, ResultBean result) {
        try {
            PaasService serv = DeployUtils.getService(instID, logKey, result);
            if (serv == null)
                return;

            if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result))
                return;

            boolean undeployResult = false;
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(serv.getServType(), logKey);
            if (serviceDeployer != null) {
                undeployResult = serviceDeployer.undeployService(instID, force, logKey, magicKey, result);
            } else {
                String err = String.format("service type not found, service_id:%s, service_type:%s", instID,
                        serv.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
            }

            if (undeployResult) {
                logger.info("service undeploy success ......");
            } else {
                result.setRetCode(CONSTS.REVOKE_NOK);
                if (result.getRetInfo() == null)
                    result.setRetInfo(CONSTS.ERR_INTERNAL);
                logger.error("service undeploy fail ......");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            DeployLog.pubFailLog(logKey, "undeploy service fail ......");
            DeployLog.pubFailLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INTERNAL);
        }
    }

    public static void deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        try {
            PaasService serv = DeployUtils.getService(servInstID, logKey, result);
            if (serv == null)
                return;

            // 服务未部署情况下不能增量部署组件, 必须先对服务进行部署
            if (DeployUtils.isServiceNotDeployed(serv, logKey, result))
                return;

            PaasInstance servInst = DeployUtils.getInstance(servInstID, logKey, result);
            if (servInst == null)
                return;
            
            PaasInstance inst = DeployUtils.getInstance(instID, logKey, result);
            if (inst == null)
                return;

            if (DeployUtils.isInstanceDeployed(inst, logKey, result))
                return;

            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasMetaCmpt cmpt = meta.getCmptById(servInst.getCmptId()); // (inst.getCmptId());

            boolean deployResult = false;
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(cmpt.getServType(), logKey);
            if (serviceDeployer != null) {
                deployResult = serviceDeployer.deployInstance(servInstID, instID, logKey, magicKey, result);
            } else {
                String err = String.format("service type not found, service_id:%s, inst_id:%s, service_type:%s",
                        servInstID, instID, cmpt.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
            }

            if (deployResult) {
                logger.info("instance deploy success ......");
            } else {
                result.setRetCode(CONSTS.REVOKE_NOK);
                if (result.getRetInfo() == null)
                    result.setRetInfo(CONSTS.ERR_INTERNAL);
                logger.error("instance deploy fail ......");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            DeployLog.pubFailLog(logKey, "deploy instance fail ......");
            DeployLog.pubFailLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INTERNAL);
        }
    }

    public static void undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        try {
            PaasInstance inst = DeployUtils.getInstance(instID, logKey, result);
            if (inst == null) {
                logger.error("instance id:{} not found", instID);
                return;
            }

            if (DeployUtils.isInstanceNotDeployed(inst, logKey, result))
                return;

            PaasInstance servInst = DeployUtils.getInstance(servInstID, logKey, result);
            if (servInst == null)
                return;

            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasMetaCmpt cmpt = meta.getCmptById(servInst.getCmptId()); // (inst.getCmptId());
            boolean undeployResult = false;
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(cmpt.getServType(), logKey);
            if (serviceDeployer != null) {
                undeployResult = serviceDeployer.undeployInstance(servInstID, instID, logKey, magicKey, result);
            } else {
                String err = String.format("service type not found, service_id:%s, inst_id:%s, service_type:%s",
                        servInstID, instID, cmpt.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
            }

            if (undeployResult) {
                logger.info("instance undeploy success ......");
            } else {
                result.setRetCode(CONSTS.REVOKE_NOK);
                if (result.getRetInfo() == null)
                    result.setRetInfo(CONSTS.ERR_INTERNAL);
                logger.error("instance undeploy fail ......");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            DeployLog.pubFailLog(logKey, "undeploy instance fail ......");
            DeployLog.pubFailLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INTERNAL);
        }
    }
    
    public static void maintainInstance(String servInstID, String instID, String servType, InstanceOperationEnum op, 
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        try {
            PaasInstance inst = DeployUtils.getInstance(instID, logKey, result);
            if (inst == null) {
                String info = String.format("instance id:%s not found", instID);
                
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(info);
                
                DeployLog.pubLog(logKey, info);
                return;
            }

            if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
                
                DeployLog.pubLog(logKey, CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
                return;
            }

            PaasInstance servInst = DeployUtils.getInstance(servInstID, logKey, result);
            if (servInst == null) {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_SERVICE_NOT_FOUND);
                
                DeployLog.pubLog(logKey, CONSTS.ERR_SERVICE_NOT_FOUND);
                return;
            }

            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasMetaCmpt cmpt = meta.getCmptById(servInst.getCmptId());
            boolean startResult = false;
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(cmpt.getServType(), logKey);
            if (serviceDeployer != null) {
                startResult = serviceDeployer.maintainInstance(servInstID, instID, servType, op, isOperateByHandle, logKey, magicKey, result);
            } else {
                String err = String.format("service deployer not found, service_id:%s, inst_id:%s, service_type:%s",
                        servInstID, instID, cmpt.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
                DeployLog.pubLog(logKey, err);
            }

            if (startResult) {
                logger.info("instance {} success ......", op.getAction());
            } else {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_INTERNAL);
                logger.error("instance {} fail ......", op.getAction());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            String failInfo = String.format("%s instance fail: %s", op.getAction(), e.getMessage());
            DeployLog.pubFailLog(logKey, failInfo);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INTERNAL);
        }
    }
    
    private static boolean isSameSSH(String instID, String nextInstID) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        String sshId = meta.getInstAttr(instID, 116).getAttrValue();  // 116 -> 'SSH_ID'
        String nextSshId = meta.getInstAttr(nextInstID, 116).getAttrValue();  // 116 -> 'SSH_ID'
        return sshId.equals(nextSshId);
    }
    
    public static void batchUpdateInst(String servInstID, String[] instIdArr, String servType, String logKey, String magicKey, ResultBean result) {
        StringBuilder successInstId = new StringBuilder("");
        for (String instId : instIdArr) {
            // 批量更新前先将实例IS_DEPLOY置为2，防止更新的过程中又被监控扫描程序拉起
            MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_WARN, result, magicKey);
        }
        
        for (int i = 0; i < instIdArr.length; ++i) {
            String instID = instIdArr[i];
            boolean loadDeployFile = false, rmDeployFile = false;
            
            if (i == 0) {
                // 第一个要拉取部署文件
                loadDeployFile = true;
                
                if (instIdArr.length > 1) {
                    String nextInstID = instIdArr[i + 1];
                    if (!isSameSSH(instID, nextInstID)) {
                        // 如果后面的与当前是部署在同一台，本次用完后不删除部署文件
                        rmDeployFile = true;
                    }
                } else {
                    rmDeployFile = true;
                }
            } else if (i < (instIdArr.length - 1)) {
                String privInstID = instIdArr[i - 1];
                String nextInstID = instIdArr[i + 1];
                if (!isSameSSH(instID, nextInstID)) {
                    // 如果后面的与当前是部署在同一台，本次用完后不删除部署文件
                    rmDeployFile = true;
                }
                if (!isSameSSH(instID, privInstID)) {
                    // 如果当前的与前面的部署在不同的机器，则要拉取
                    loadDeployFile = true;
                }
            } else {
                // 最后一个要删除部署文件
                rmDeployFile = true;
                
                String privInstID = instIdArr[i - 1];
                if (!isSameSSH(instID, privInstID)) {
                    // 如果当前的与前面的部署在不同的机器，则要拉取
                    loadDeployFile = true;
                }
            }
            
            // String s = String.format("%d %s %s", i, (loadDeployFile ? "true" : false), (rmDeployFile ? "true" : "false"));
            // DeployLog.pubLog(logKey, s);
            
            try {
                PaasInstance inst = DeployUtils.getInstance(instID, logKey, result);
                if (inst == null) {
                    String info = String.format("instance id:%s not found", instID);
                    result.setRetCode(CONSTS.REVOKE_NOK);
                    DeployLog.pubLog(logKey, info);
                    break;
                }

                if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
                    result.setRetCode(CONSTS.REVOKE_NOK);
                    DeployLog.pubLog(logKey, CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
                    break;
                }

                PaasInstance servInst = DeployUtils.getInstance(servInstID, logKey, result);
                if (servInst == null) {
                    result.setRetCode(CONSTS.REVOKE_NOK);
                    DeployLog.pubLog(logKey, CONSTS.ERR_SERVICE_NOT_FOUND);
                    break;
                }

                CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
                PaasMetaCmpt cmpt = meta.getCmptById(servInst.getCmptId());
                boolean maintainResult = false;
                ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(cmpt.getServType(), logKey);
                InstanceOperationEnum op = InstanceOperationEnum.INSTANCE_OPERATION_UPDATE;
                if (serviceDeployer != null) {
                    maintainResult = serviceDeployer.updateInstanceForBatch(servInstID, instID, servType, 
                            loadDeployFile, rmDeployFile, true, logKey, magicKey, result);
                } else {
                    String err = String.format("service deployer not found, service_id:%s, inst_id:%s, service_type:%s",
                            servInstID, instID, cmpt.getServType());
                    result.setRetCode(CONSTS.REVOKE_NOK);
                    result.setRetInfo(err);
                    DeployLog.pubLog(logKey, err);
                    break;
                }

                if (maintainResult) {
                    logger.info("instance {} success ......", op.getAction());
                    
                    if (successInstId.length() > 0)
                        successInstId.append(",");
                    
                    successInstId.append(instID);
                } else {
                    result.setRetCode(CONSTS.REVOKE_NOK);
                    logger.error("instance {} fail ......", op.getAction());
                    break;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                String failInfo = String.format("update instance fail: %s", e.getMessage());
                DeployLog.pubFailLog(logKey, failInfo);
                result.setRetCode(CONSTS.REVOKE_NOK);
                
                break;
            }
        }
        
        result.setRetInfo(successInstId.toString());
    }
    
    public static void checkInstanceStatus(String servInstID, String instID, String servType, String magicKey, ResultBean result) {
        try {
            PaasInstance inst = DeployUtils.getInstance(instID, null, result);
            if (inst == null) {
                String info = String.format("instance id:%s not found", instID);
                
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(info);
                
                return;
            }

            if (DeployUtils.isInstanceNotDeployed(inst, null, result)) {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
                return;
            }

            PaasInstance servInst = DeployUtils.getInstance(servInstID, null, result);
            if (servInst == null) {
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_SERVICE_NOT_FOUND);
                return;
            }

            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasMetaCmpt cmpt = meta.getCmptById(servInst.getCmptId());
            ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(cmpt.getServType(), null);
            if (serviceDeployer != null) {
                serviceDeployer.checkInstanceStatus(servInstID, instID, servType, magicKey, result);
            } else {
                String err = String.format("service deployer not found, service_id:%s, inst_id:%s, service_type:%s",
                        servInstID, instID, cmpt.getServType());
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(err);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(String.format("checkInstanceStatus instance fail: %s", e.getMessage()));
        }
    }
    
    public static void getDeployLog(String logKey, ResultBean result) {
        String log = DeployLog.getLog(logKey);
        result.setRetCode(CONSTS.REVOKE_OK);
        result.setRetInfo(log);
    }
    
    public static void getAppLog(String servId, String instId, String logType, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance instance = meta.getInstance(instId);
        if (instance == null || !instance.isDeployed()) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_DEPLOYED);
        }
        
        String cmptName = meta.getInstCmptName(instId);
        String logFile = getSmsLogFile(meta, instance, cmptName, logType);
        
        String sshId = meta.getInstAttr(instance.getInstId(), 116).getAttrValue();  // 116, 'SSH_ID'
        PaasSsh ssh = meta.getSshById(sshId);
        
        SSHExecutor ssh2 = new SSHExecutor(ssh);
        if (!DeployUtils.initSsh2(ssh2, "", result)) return;
        try {
            String context = ssh2.tail(logFile, 100, null);
            String replacedContext = context.replaceAll(CONSTS.LINE_END, CONSTS.HTML_LINE_END);
            
            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(replacedContext);
        } catch (SSHException e) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage() + CONSTS.HTML_LIEN_END);
        } finally {
            ssh2.close();
        }
    }
    
    private static String getSmsLogFile(CmptMeta meta, PaasInstance instance, String cmptName, String logType) {
        String result = null;
        String fullFilePath = null;
        String logFileName = getLogFileName(meta, instance, cmptName, logType);
        
        switch (cmptName) {
        case CONSTS.APP_SMS_SERVER:
            fullFilePath = String.format("smsserver_%s/logs/%s", instance.getInstId(), logFileName);
            break;
        case CONSTS.APP_SMS_PROCESS:
            String processor = meta.getInstAttr(instance.getInstId(), 205).getAttrValue();  // 205, 'PROCESSOR'
            fullFilePath = String.format("smsprocess_%s/logs/%s", processor, logFileName);
            break;
        case CONSTS.APP_SMS_CLIENT:
            String chanGrp = meta.getInstAttr(instance.getInstId(), 205).getAttrValue();  // 205, 'PROCESSOR'
            fullFilePath = String.format("smsclient-standard_%s/logs/%s", chanGrp, logFileName);
            break;
        case CONSTS.APP_SMS_BATSAVE:
            String batSaveGrp = meta.getInstAttr(instance.getInstId(), 205).getAttrValue(); // 205, 'PROCESSOR'
            String dbInstId = meta.getInstAttr(instance.getInstId(), 213).getAttrValue();   // 213, 'DB_INST_ID'
            fullFilePath = String.format("smsbatsave_%s_%s/logs/%s", batSaveGrp, dbInstId, logFileName);
            break;
        case CONSTS.APP_SMS_STATS:
            fullFilePath = String.format("smsstatistics_%s/logs/%s", instance.getInstId(), logFileName);
            break;
        default:
            break;
        }
        
        if (fullFilePath != null) {
            result = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, fullFilePath);
        }
        
        return result;
    }
    
    private static String getLogFileName(CmptMeta meta, PaasInstance instance, String cmptName, String logType) {
        String logFileName = null;
        
        if (logType.equals(CONSTS.LOG_TYPE_STDOUT))
            return "stdout.log";
        
        switch (cmptName) {
        case CONSTS.APP_SMS_SERVER:
            logFileName = String.format("smsserver-%s.log", logType);
            break;
        case CONSTS.APP_SMS_PROCESS:
            String processor = meta.getInstAttr(instance.getInstId(), 205).getAttrValue();  // 205, 'PROCESSOR'
            logFileName = String.format("smsprocess-%s-%s.log", logType, processor);
            break;
        case CONSTS.APP_SMS_CLIENT:
            String chanGrp = meta.getInstAttr(instance.getInstId(), 205).getAttrValue();  // 205, 'PROCESSOR'
            logFileName = String.format("smsclient-%s-%s.log", logType, chanGrp);
            break;
        case CONSTS.APP_SMS_BATSAVE:
            String batSaveGrp = meta.getInstAttr(instance.getInstId(), 205).getAttrValue(); // 205, 'PROCESSOR'
            logFileName = String.format("smsbatsave-%s-%s.log", logType, batSaveGrp);
            break;
        case CONSTS.APP_SMS_STATS:
            logFileName = String.format("smsstatistics-%s.log", logType);
            break;
        default:
            break;
        }
        
        return logFileName;
    }

}
