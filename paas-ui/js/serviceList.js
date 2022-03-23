var serviceListParams = {};
var $serverList = $("#service_list");
var arrServVersion = {};

function initPage(){
    loadServiceList();
}

function loadServiceList() {
    $serverList.mTable( {
        url: Url.serverList.loadServiceList,
        countUrl: Url.serverList.loadServiceListCount,
        queryParams: serviceListParams,
        striped : true,
        pagination : true,
        pageSize : 20,
        pageNumber : 1,
        columns : [{
            checkbox:true,
            format:function(_value, row, _index) {  //value是值，row是当前的行记录，index是行数 从0开始
                if (row.IS_DEPLOYED === '1') {
                    return {
                        disabled:true
                    }
                }
            }
        },{
            field : "INST_ID",
            title : "服务ID",
        },{
            field : "SERV_NAME",
            title : "服务名",
        }, {
            field : "SERV_TYPE",
            title : "服务类别",
        }, {
            field : "VERSION",
            title : "版本",
        }, {
            field : "SERV_CLAZZ",
            title : "服务大类",
            hided : true,
        }, {
            field : "IS_DEPLOYED",
            title : "部署情况",
            format:function(value, _row, _index){
            	return value=='0' ? '未部署' : '已部署';
            }
        } ,{
            field : "IS_PRODUCT",
            title : "是否生产环境",
            format:function(value, _row, _index){
            	return value=='1' ? '是' : '否';
            }
        } ,{
            title : "操作",
            isButtonColumn:true,
            buttons:[{
                text:"修改",
                format:function(_value, row){
                    if(row.IS_DEPLOYED == 1) {
                        return { hided:true };
                    }
                },
                onClick:function(_button, row, _index) {
                    $('#MOD_INST_ID').val(row.INST_ID);
                    $('#MOD_SERV_NAME').val(row.SERV_NAME);
                    setVersionSel(row.SERV_TYPE ,$('#MOD_SERV_VERSION'));
                    $('#MOD_SERV_VERSION').val(row.VERSION);
                    $('#MOD_IS_PRODUCT').val(row.IS_PRODUCT);
                    $('#modService').modal("show");
                    $(".modal-backdrop").appendTo($("#mainContent"));
                }
            },{
                text:"管理",
                onClick:function(_button, row, _index) {
                    $mainContainer.load("serviceManage.html",function() {
                        init(row.INST_ID, row.SERV_NAME, row.SERV_TYPE, row.SERV_CLAZZ,row.IS_PRODUCT,row.VERSION);
                    });
                }
            },{
                text:"修改",
                format:function(_value, row){
                    if (row.IS_DEPLOYED == '0') {
                        return { hided:false };
                    } else {
                        if (row.SERV_TYPE == 'SMS_GATEWAY' || row.SERV_TYPE == 'SMS_QUERY_SERVICE') {
                            return { hided:false };
                        } else {
                            return { hided:true };
                        }
                    }
                },
                onClick:function(_button, row, _index) {
                    $('#MOD_INST_ID1').val(row.INST_ID);
                    setVersionSel(row.SERV_TYPE, $('#MOD_SERV_VERSION1'));
                    $('#MOD_SERV_VERSION1').val(row.VERSION);
                    $('#modServiceVersion').modal("show");
                    $(".modal-backdrop").appendTo($("#mainContent"));
                }
            },{
                text:"执行计划",
                //配置隐藏属性
                format:function(_value, row, _index) {
                    if(row.SERV_TYPE != 'DB' || row.IS_DEPLOYED!=1) {
                        return { hided:true };
                    }
                },
                onClick:function(_button, row, _index) {
                    $mainContainer.load("sqlExplain.html",function() {
                        init(row.SERV_NAME, row.INST_ID);
                    })
                }
            },{
                text:"监控",
                //配置隐藏属性
                format:function(_value,row){
                    if(row.IS_DEPLOYED!=1) {
                        return { hided:true };
                    }
                },
                onClick:function(_button,row) {
                    if (row.SERV_TYPE == 'MQ_ROCKETMQ') {
                        showRocketMQDashboard(row.INST_ID);
                    } else if(row.SERV_TYPE == 'CACHE_REDIS_CLUSTER') {
                        $mainContainer.load("cacheRedisClusterMonitor.html", function() {
                            initInstId(row.INST_ID);
                        });
                    } else if(row.SERV_TYPE == 'CACHE_REDIS_CLUSTER') {
                        $mainContainer.load("cacheRedisMonitor.html", function() {
                            init(row.INST_ID);
                        });
                    } else if(row.SERV_TYPE == 'DB_TIDB') {
                        showTiDBDashboard(row.INST_ID);
                    } else if(row.SERV_TYPE == 'DB_CLICKHOUSE') {
                        showClickHouseDashboard(row.INST_ID);
                    } else if(row.SERV_TYPE == 'DB_VOLTDB') {
                        showVoltDBDashboard(row.INST_ID);
                    } else if(row.SERV_TYPE == 'MQ_PULSAR') {
                        showPulsarDashboard(row.INST_ID);
                    } else if(row.SERV_TYPE == 'DB_YUGABYTEDB') {
                        showYugaByteDashboard(row.INST_ID);
                    }
                }
            },{
                text:"元数据",
                //配置隐藏属性
                onClick:function(_button, row, _index) {
                    $mainContainer.load("metadata.html", function() {
                        searchByRemote(row.INST_ID);
                    })
                }
            }]
        }]
    });
}

(function (){
    var data = "";
    $.ajax({
        url: Url.serverList.getServTypeVerList,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                arrServVersion = result.metaCmptVerMap;
            }
        }
    });
})()

function showClickHouseDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getClickHouseDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "clickhouse dashboard null");
            }
        }
    });
}

function showVoltDBDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getVoltDBDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "voltdb dashboard null");
            }
        }
    });
}

function showRocketMQDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getRocketMQDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "rocketmq console null");
            }
        }
    });
}

function showTiDBDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getTiDBDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "tidb dashboard null");
            }
        }
    });
}

function showPulsarDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getPulsarDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "pulsar dashboard null");
            }
        }
    });
}

function showYugaByteDashboard(servId) {
    var data = {};
    data.SERV_INST_ID = servId;
    
    if (data.SERV_INST_ID.trim() == "") {
        Util.alert("error", "SERV_INST_ID不能为空！");
        return;
    }
    
    $.ajax({
        url: Url.serverList.getYBDashboardAddr,
        type:"post",
        async: false,
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                var url = result.RET_INFO;
                window.open(url);
            } else {
                Util.alert("error", "yugabyte dashboard null");
            }
        }
    });
}

function showAddService() {
    $('#newServiceHeader').text("新增服务");
    $('#SERV_CLAZZ').attr("disabled", false);
    $('#SERV_NAME').val("");
    $('#IS_PRODUCT').val(0);
    $('#USER').val("");
    $('#PASSWORD').val("");
    $('#newService').modal("show");
    $(".modal-backdrop").appendTo($("#mainContent"));
}

function delService() {
    var services = $serverList.mTable("getSelections");
    if (services.length<1) {
        Util.alert("warn", "请选择服务");
        return;
    } else if (services.length>1) {
        Util.alert("warn", "一次只能删除一个服务");
        return;
    }

    Util.confirm("确认删除服务吗？", {
        btn: ['是','否'],
        title: "确认"
    }, function() {
        var services = $serverList.mTable("getSelections");
        var data = {};
        data.INST_ID = services[0].INST_ID;
        var loading = $('#loadingDiv');

        $.ajax({
            url: Url.serverList.delService,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data),
            success: function(result) {
                if (result.RET_CODE == 0) {
                    Util.msg("删除成功");
                    $serverList.mTable("refresh");
                } else {
                    Util.alert("error", "删除服务集群失败！"+result.RET_INFO);
                }
            }
        });
    });
}

function addService() {
    var data = {};
    var loading = $('#loadingDiv');
    data.SERV_NAME  = $('#SERV_NAME').val();
    data.SERV_CLAZZ = $('#SERV_CLAZZ').val();
    data.SERV_TYPE  = $('#SERV_TYPE').val();
    data.VERSION   = $('#SERV_VERSION').val();
    data.IS_PRODUCT = $('#IS_PRODUCT').val();
    data.USER       = $('#USER').val();
    data.PASSWORD   = $('#PASSWORD').val();

    if (data.SERV_NAME.trim() == "") {
        Util.alert("error", "服务名不能为空！");
        return;
    }
    
    if (data.USER.trim() == "") {
        Util.alert("error", "用户名不能为空！");
        return;
    }
    
    if (data.USER.trim() == "") {
        Util.alert("error", "密码不能为空！");
        return;
    }

    $.ajax({
        url:  Url.serverList.addService,
        type : "post",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                $('#newService').modal("hide");
                $serverList.mTable("refresh");
            } else {
                Util.alert("error", "新增服务失败！"+result.RET_INFO);
            }
        }
    });
}

function modService() {
    var data = {};
    var loading = $('#loadingDiv');
    data.INST_ID    = $('#MOD_INST_ID').val();
    data.SERV_NAME  = $('#MOD_SERV_NAME').val();
    data.VERSION  = $('#MOD_SERV_VERSION').val();
    data.IS_PRODUCT = $('#MOD_IS_PRODUCT').val();

    $.ajax({
        url:  Url.serverList.modService,
        type : "post",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                $('#modService').modal("hide");
                $serverList.mTable("refresh");
            } else {
                Util.alert("error", "修改服务信息失败！"+result.RET_INFO);
            }
        }
    });
}

function modServiceVersion() {
    var data = {};
    var loading = $('#loadingDiv');
    data.INST_ID    = $('#MOD_INST_ID1').val();
    data.VERSION  = $('#MOD_SERV_VERSION1').val();

    $.ajax({
        url:  Url.serverList.modServiceVersion,
        type : "post",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                Util.alert("info", "版本修改成功！");
                $('#modServiceVersion').modal("hide");
                $serverList.mTable("refresh");
            } else {
                Util.alert("error", "修改服务版本信息失败！"+result.RET_INFO);
            }
        }
    });
}

function onServClazzSel(typeSel, clazzSel, hide, versionSel) {
    var servClazz = clazzSel.val();
    typeSel[0].options.length = 0;
    // versionSel[0].options.length = 0;
    if (!hide) typeSel.append(`<option selected value = "">服务类型（----）</option>`)
    switch (servClazz) {
        case 'SMS':
            typeSel.append(`<option value ="SMS_GATEWAY">SMS_GATEWAY</option>`);
            typeSel.append(`<option value ="SMS_QUERY_SERVICE">SMS_QUERY_SERVICE</option>`);
            if (hide) setVersionSel("SMS_GATEWAY", versionSel);
            break;
        case 'DB':
            typeSel.append(`<option value ="DB_TIDB">DB_TIDB</option>`);
            typeSel.append(`<option value ="DB_YUGABYTEDB">DB_YUGABYTEDB</option>`);
            typeSel.append(`<option value ="DB_TDENGINE">DB_TDENGINE</option>`);
            typeSel.append(`<option value ="DB_ORACLE_DG">DB_ORACLE_DG</option>`);
            typeSel.append(`<option value ="DB_CLICKHOUSE">DB_CLICKHOUSE</option>`);
            typeSel.append(`<option value ="DB_VOLTDB">DB_VOLTDB</option>`);
            if (hide) setVersionSel("DB_TIDB", versionSel);
            break;
        case 'MQ':
            typeSel.append(`<option value ="MQ_PULSAR">MQ_PULSAR</option>`);
            typeSel.append(`<option value ="MQ_ROCKETMQ">MQ_ROCKETMQ</option>`);
            if (hide) setVersionSel("MQ_PULSAR", versionSel);
            break;
        case 'CACHE':
            typeSel.append(`<option value ="CACHE_REDIS_CLUSTER">CACHE_REDIS_CLUSTER</option>`);
            typeSel.append(`<option value ="CACHE_REDIS_MASTER_SLAVE">CACHE_REDIS_MASTER_SLAVE</option>`);
            typeSel.append(`<option value ="CACHE_REDIS_HA_CLUSTER">CACHE_REDIS_HA_CLUSTER</option>`);
            if (hide) setVersionSel("CACHE_REDIS_CLUSTER", versionSel);
            break;
        case 'SERVERLESS':
            typeSel.append(`<option value ="SERVERLESS_APISIX">SERVERLESS_APISIX</option>`);
            if (hide) setVersionSel("SERVERLESS_APISIX", versionSel);
            break;
        default:
            if (hide) typeSel.append(`<option selected value = "">服务类型（----）</option>`);
    }
}

function onServTypeSel(typeSel, versionSel) {
    setVersionSel(typeSel.val(), versionSel);
}

function setVersionSel(servType, versionSel) {    
    versionSel[0].options.length = 0;
    var strServVersion = arrServVersion[servType];
    var arrVersion = strServVersion.VERSION.split(",");
    for (var i = 0; i < arrVersion.length;i++) {
        versionSel.append("`<option value =" + arrVersion[i] + ">" + arrVersion[i] + "</option>`");
    }
}

function searchService() {
    serviceListParams.SERV_INST_ID = $('#S_SERV_INST_ID').val();
    serviceListParams.SERV_NAME = $('#S_SERV_NAME').val();
    serviceListParams.SERV_CLAZZ = $('#S_SERV_CLAZZ').val();
    serviceListParams.SERV_TYPE = $('#S_SERV_TYPE').val();
    $serverList.mTable("reload", {
        queryParams: serviceListParams,
        pageNumber: 1
    });
}
