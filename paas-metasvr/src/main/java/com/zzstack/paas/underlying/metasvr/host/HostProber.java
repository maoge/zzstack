package com.zzstack.paas.underlying.metasvr.host;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SSHExecutor;
import com.zzstack.paas.underlying.metasvr.exception.SSHException;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.exception.PaasCollectException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.paas.PaasHttpOperationUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HostProber implements Prober {

    private static final Logger logger = LoggerFactory.getLogger(HostProber.class);
    private JsonObject jsonToReport = null;

    @Override
    public void doCollect(JsonObject topoJson) {
        JsonObject hostContainerJson = topoJson.getJsonObject(FixHeader.HEADER_HOST_CONTAINER);
        JsonObject hostNodeContainer = hostContainerJson.getJsonObject(FixHeader.HEADER_HOST_NODE_CONTAINER);
        JsonArray hostNodesJsonArray = hostNodeContainer.getJsonArray(FixHeader.HEADER_HOST_NODE);
        int hostNodeSize = hostNodesJsonArray.size();
        Map<String, SSHExecutor> hostNodes = new HashMap<>();
        for (int i = 0; i < hostNodeSize; i++) {
            JsonObject hostNode = hostNodesJsonArray.getJsonObject(i);
            String instId = hostNode.getString(FixHeader.HEADER_INST_ID);
            String ip = hostNode.getString(FixHeader.HEADER_IP);
            int port = hostNode.getInteger(FixHeader.HEADER_PORT);
            String userName = hostNode.getString(FixHeader.HEADER_USER_NAME);
            String password = hostNode.getString(FixHeader.HEADER_PASSWORD);
            hostNodes.put(instId, new SSHExecutor(userName, password, ip, port));
        }
        try {
            collectHostInfo(hostNodes);
        } catch (PaasSdkException | SSHException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void doReport() throws PaasSdkException {
        if (jsonToReport != null) {
            Map<String, String> postHeads = new HashMap<>();
            postHeads.put("CONTENT-TYPE", "application/json");
            try {
                PaasHttpOperationUtils.postCollectData("htttp://127.0.0.1:9090/paas/statistic/saveHostInfo", postHeads, jsonToReport.toString());
            } catch (IOException e) {
                logger.error("doReport caught exception:{}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void doAlarm() throws PaasCollectException {
        // TODO Auto-generated method stub

    }

    @Override
    public void doRecover() throws PaasCollectException {
        // TODO Auto-generated method stub

    }

    public void collectHostInfo(Map<String, SSHExecutor> hostNodes) throws PaasSdkException, SSHException {
        // 组装主机监控数据(内存、cpu、硬盘、网络)
        JsonObject hostInfo = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        long currentStamp = System.currentTimeMillis();
        for (Map.Entry<String, SSHExecutor> entry : hostNodes.entrySet()) {
            // host list operates on master node
            SSHExecutor sshExecutor = entry.getValue();
            if (sshExecutor != null) {
                ResultBean resultBean = new ResultBean();
                boolean loginResult = DeployUtils.initSsh2(sshExecutor, "", resultBean);
                if (!loginResult) {
                    logger.warn("");
                    continue;
                }
                String instId = entry.getKey();
                String memCmd = "free | head -n 2 | tail -n 1 |awk '{print $2,$3}';";
                String cpuCmd = "top | head -n 6|grep Cpu|awk '{print $2,$4,$8}';";
                String diskCmd = "df | grep /home|awk '{print $2,$3,$4}';";
                String userDiskCmd = "du -s $HOME|awk '{print $1}';";
                String networkCmd = "sar -n DEV 1 1 | head -n 4 | tail -n 1 | awk '{print $5,$6}' ";
                String cmd =
                        // 内存 总量、已用
                        memCmd +
                        // cpu（us、sys、id)用户、系统、空闲
                        cpuCmd +
                        // 硬盘
                        // 总量、已用、可用 $HOME
                        diskCmd +
                        // 当前用户已用硬盘
                        userDiskCmd +
                        // 网络 网卡名、输入kb/s、输出kb/s
                        networkCmd;
                String resultInfo = sshExecutor.generalCommand(cmd);
                int startIndex = resultInfo.indexOf(",$6}'");
                int lastIndex = resultInfo.lastIndexOf("[");
                resultInfo = resultInfo.substring(startIndex + 5, lastIndex);
                sshExecutor.close();
                String[] res = resultInfo.replace("\r", "").trim().split("\n");
                // 提取字符串中主机的关键指标
                for (String re : res) {
                    System.out.println(re);
                }
                infoTransformHost(res, currentStamp, instId, jsonArray);
            }
        }
        hostInfo.put(FixHeader.HEADER_HOST_INFO_JSON_ARRAY, jsonArray);
        jsonToReport = new JsonObject();
        jsonToReport.put(FixHeader.HEADER_HOST_DATA, hostInfo);
    }

    private void infoTransformHost(String[] res, long currentStamp, String instId, JsonArray jsonArray) {
        if (res != null && res.length == 5) {
            JSONObject redis = new JSONObject();
            redis.put(FixHeader.HEADER_TS, currentStamp);
            // 应用的实例ID
            redis.put(FixHeader.HEADER_INST_ID, instId);
            if (StringUtils.isNotBlank(res[0])) {
                String[] mem = res[0].split(" ");
                redis.put(FixHeader.HEADER_MEMORY_TOTAL, mem[0]);
                redis.put(FixHeader.HEADER_MEMORY_USED, mem[1]);
            }
            if (StringUtils.isNotBlank(res[1])) {
                String[] cpu = res[1].split(" ");
                redis.put(FixHeader.HEADER_USED_CPU_USER, cpu[0]);
                redis.put(FixHeader.HEADER_USED_CPU_SYS, cpu[1]);
                redis.put(FixHeader.HEADER_CPU_IDLE, cpu[2]);
            }
            if (StringUtils.isNotBlank(res[2])) {
                String[] disk = res[2].split(" ");
                redis.put(FixHeader.HEADER_TOTAL_DISK, disk[0]);
                redis.put(FixHeader.HEADER_USED_DISK, disk[1]);
                redis.put(FixHeader.HEADER_UNUSED_DISK, disk[2]);
            }
            if (StringUtils.isNotBlank(res[3])) {
                String userUsedDisk = res[3];
                redis.put(FixHeader.HEADER_USER_USED_DISK, userUsedDisk);
            }
            if (StringUtils.isNotBlank(res[4])) {
                String[] network = res[4].split(" ");
                redis.put(FixHeader.HEADER_INPUT_BANDWIDTH, network[0]);
                redis.put(FixHeader.HEADER_OUTPUT_BANDWIDTH, network[1]);
            }
            jsonArray.add(redis);
        }
    }

    public static void main(String[] args) throws PaasSdkException, SSHException {
        HostProber hostProber = new HostProber();
        Map<String, SSHExecutor> hostNodes = new HashMap<>();
        SSHExecutor sshExecutor = new SSHExecutor("sms", "wlwx2021", "172.20.0.171", 22);
        hostNodes.put("1", sshExecutor);
        hostProber.collectHostInfo(hostNodes);
    }

}
