var rootUrl=location.protocol + "//" + location.host + "/",
    $mainContainer = $("#mainContent");

var STATUS_NEW = "-1",
    STATUS_UNDEPLOYED = "0";

var Url = window.Url = {
    acc : {
        login :                       rootUrl + "paas/account/login",
        modPwd :                      rootUrl + "paas/account/modPassWord",
        getOpLogCnt:                  rootUrl + "paas/account/getOpLogCnt",
        getOpLogList:                 rootUrl + "paas/account/getOpLogList"
    },
    serverList:{
        loadServiceList :             rootUrl + "paas/metadata/getServiceList",
        loadServiceListCount :        rootUrl + "paas/metadata/getServiceCount",
        addService :                  rootUrl + "paas/metadata/addService",
        delService :                  rootUrl + "paas/metadata/delService",
        modService :                  rootUrl + "paas/metadata/modService",
        modServiceVersion :           rootUrl + "paas/metadata/modServiceVersion",
        getServTypeVerList :          rootUrl + "paas/metadata/getServTypeVerList",
        getClickHouseDashboardAddr :  rootUrl + "paas/metadata/getClickHouseDashboardAddr",
        getVoltDBDashboardAddr :      rootUrl + "paas/metadata/getVoltDBDashboardAddr",
        getRocketMQDashboardAddr :    rootUrl + "paas/metadata/getRocketMQDashboardAddr",
        getTiDBDashboardAddr :        rootUrl + "paas/metadata/getTiDBDashboardAddr",
        getPulsarDashboardAddr :      rootUrl + "paas/metadata/getPulsarDashboardAddr",
        getYBDashboardAddr:           rootUrl + "paas/metadata/getYBDashboardAddr",
        adjustSmsABQueueWeightInfo :  rootUrl + "paas/metadata/adjustSmsABQueueWeightInfo",
        switchSmsDBType:              rootUrl + "paas/metadata/switchSmsDBType",
        reloadMetaDataInfo:           rootUrl + "paas/metadata/reloadMetaData"
    },
    alarm : {
        getAlarmList :                rootUrl + "paas/alarm/getAlarmList",
        getAlarmCount :               rootUrl + "paas/alarm/getAlarmCount",
        clearAlarm :                  rootUrl + "paas/alarm/clearAlarm"
    },
    monitorList : {
        getJvmInfo :                  rootUrl + "paas/statistic/getJvmInfo",
        getRedisInstanceInfo :        rootUrl + "paas/statistic/getRedisInstanceInfo",
        getRedisHaServiceInfo :       rootUrl + "paas/statistic/getRedisHaServiceInfo",
        getRedisServNodes :           rootUrl + "paas/statistic/getRedisServNodes",
        getRocketMQServBrokers :      rootUrl + "paas/statistic/getRocketMQServBrokers",
        getTopicNameByInstId :        rootUrl + "paas/statistic/getTopicNameByInstId",
        getConsumeGroupByTopicName :  rootUrl + "paas/statistic/getConsumeGroupByTopicName",
        getRocketmqInfo :             rootUrl + "paas/statistic/getRocketmqInfo"
    }
};
