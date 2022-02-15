package com.zzstack.paas.underlying.metasvr.autodeploy;

import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

public interface ServiceDeployer {

    boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result);

    boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result);

    boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result);

    boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result);

    boolean maintainInstance(String servInstID, String instID, String servType, InstanceOperationEnum op, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result);
    
    boolean updateInstanceForBatch(String servInstID, String instID, String servType, boolean loadDeployFile, boolean rmDeployFile, 
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result);

    boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey, ResultBean result);

}
