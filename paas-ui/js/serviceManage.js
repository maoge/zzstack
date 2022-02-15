var plate;

function init(id, name, type, clazz, isProduct ,version) {

    var $canvas = $("#canvas");
    $('#service_name').text(name);

    $canvas.attr("width",$canvas.width());
    $canvas.attr("height",$canvas.height());
    //初始化组件工具箱
    collapseToolBar();
    
    switch(type) {
        case "CACHE_REDIS_CLUSTER":
            $('#service_type').text("CACHE_REDIS集群管理");
            plate = new Component.CacheRedisClusterPlate(rootUrl, id, name, $("#canvas")[0]);
            $("#cache-deploy").show();
            $("#Cache-Redis").show();
            break;

        case "CACHE_REDIS_MASTER_SLAVE":
            $('#service_type').text("CACHE_REDIS_HA");
            plate = new Component.CacheRedisMSPlate(rootUrl, id, name, $("#canvas")[0]);
            $("#cache-deploy").show();
            $("#Cache-Redis").show();
            break;

        case "CACHE_REDIS_HA_CLUSTER":
            $('#service_type').text("CACHE_REDIS管理");
            plate = new Component.CacheRedisHaClusterPlate(rootUrl, id, name, $("#canvas")[0],isProduct);
            $("#cache-deploy").show();
            $("#Cache-Redis").show();
            break;

        case "MQ_ROCKETMQ":
            $('#service_type').text("MQ服务");
            plate = new Component.MQRocketMQPlate(rootUrl, id, name, $("#canvas")[0],isProduct);
            $("#mq-deploy").show();
            $("#MQ-ROCKETMQ").show();
            break;

        case "MQ_PULSAR":
            $('#service_type').text("MQ服务");
            plate = new Component.MQPulsarPlate(rootUrl, id, name, $("#canvas")[0],isProduct);
            $("#mq-deploy").show();
            $("#MQ-PULSAR").show();
            break;

        case "DB_TIDB":
            $('#service_type').text("DB_TIDB集群管理");
            plate = new Component.DBTidbPlate(rootUrl, id, name, $("#canvas")[0]);
            $("#db-deploy").show();
            $("#DB-TiDB").show();
            break;

        case "DB_YUGABYTEDB":
            $('#service_type').text("DB_YUGABYTEDB集群管理");
            plate = new Component.DBYugaBytePlate(rootUrl, id, name, $("#canvas")[0]);
            $("#db-deploy").show();
            $("#DB_YUGABYTEDB").show();
            break;

        case "DB_TDENGINE":
            $('#service_type').text("DB服务");
            plate = new Component.DBTDEnginePlate(rootUrl, id, name, $("#canvas")[0],isProduct);
            $("#db-deploy").show();
            $("#DB_TDENGINE").show();
            break;

        case "DB_ORACLE_DG":
            $('#service_type').text("DB服务");
            plate = new Component.DBOracleDGPlate(rootUrl, id, name, $("#canvas")[0], isProduct, true);
            $("#db-deploy").show();
            $("#DB_ORACLE_DG").show();
           break;

        case "DB_CLICKHOUSE":
            $('#service_type').text("DB服务");
            plate = new Component.DBClickHousePlate(rootUrl, id, name, $("#canvas")[0], isProduct, true);
            $("#db-deploy").show();
            $("#DB_CLICKHOUSE").show();
            break;

        case "DB_VOLTDB":
            $('#service_type').text("DB服务");
            plate = new Component.DBVoltDBPlate(rootUrl, id, name, $("#canvas")[0], isProduct, true);
            $("#db-deploy").show();
            $("#DB_VOLTDB").show();
            break;

        case "SERVERLESS_APISIX":
            $('#service_type').text("serverless网关");
            plate = new Component.ServerlessAPISixPlate(rootUrl, id, name, $("#canvas")[0], isProduct);
            $("#serverless-deploy").show();
            $("#SERVERLESS-APISIX").show();
            break;

        case "SMS_GATEWAY":
            $('#service_type').text("SMS网关");
            plate = new Component.SMSGwPlate(rootUrl, id, name, $("#canvas")[0], isProduct, version);
            $("#sms-deploy").show();
            $("#SMS_GATEWAY").show();
            break;

        case "SMS_QUERY_SERVICE":
            $('#service_type').text("SMS查询服务");
            plate = new Component.SMSQueryPlate(rootUrl, id, name, $("#canvas")[0], isProduct, version);
            $("#sms-deploy").show();
            $("#SMS_QUERY_SERVICE").show();
            break;
    }
    
    initDragEvent();
}

function collapseToolBar() {
    $("#DB-TiDB").hide();
    $("#DB_YUGABYTEDB").hide();
    $("#DB_TDENGINE").hide();
    $("#DB_ORACLE_DG").hide();
    $("#DB_CLICKHOUSE").hide();
    $("#DB_VOLTDB").hide();

    $("#MQ-ROCKETMQ").hide();
    $("#MQ-PULSAR").hide();

    $("#Cache-Redis").hide();

    $("#SERVERLESS-APISIX").hide();

    $("#SMS_GATEWAY").hide();
    $("#SMS_QUERY_SERVICE").hide();

    $("#db-deploy").hide();
    $("#mq-deploy").hide();
    $("#cache-deploy").hide();

    $("#serverless-deploy").hide();
    $("#sms-deploy").hide();
}

/**
 * 初始化组件拖动事件
 */
function initDragEvent() {
    //为每一个部署组件绑定拖动事件
    $("div[draggable='true']").each(function() {
        this.ondragstart = function (e) {
            e = e || window.event;
            var dragSrc = this;
            var datatype = $(this).attr("id");
            try {
                //IE只允许KEY为text和URL
                e.dataTransfer.setData('text', datatype);
            } catch (ex) {
                console.log(ex);
            }
        };
    });

    //阻止默认事件
    $("#mainContent")[0].ondragover = function (e) {
        e.preventDefault();
        return false;
    };

    //创建节点
    $("#mainContent")[0].ondrop = function (e) {
        e = e || window.event;
        var datatype = e.dataTransfer.getData("text");
        if (datatype) {
            var x = e.offsetX;
            var y = e.offsetY;
            plate.newComponent(x, y, datatype);
        }
        if (e.preventDefault()) {
            e.preventDefault();
        }
        if (e.stopPropagation()) {
            e.stopPropagation();
        }
    }
}

var smsGwId, abQueveId, initAbQueueWeight;
var pro2 = $('.progress2').Progress({
    val: 0,
    size: 10, //设置默认滑块高度
    drag: true, //默认是否能够拖拽
    toFixed: 0, //精准数值设置，当滑块长度过长时可以设置此参数调整改val的改变频率，默认值0不设置，建议最大设置为2。
    tip: false, //是否显示数值提示
    title: false, //是否鼠标滑入滑块时显示数值，默认为false
    direction: 'horizontal', //设置显示方向 默认horizontal 水平 vertical 垂直
    getVal: function(res) {
      //获取滑块val值
      $("#aQueueWeight").text(res);
      $("#bQueueWeight").text(100 - res);
    }
})

//保存A/B Queue权重
function adjustSmsABQueueWeightInfo(){
    if(initAbQueueWeight.RedisClusterA.WEIGHT == $("#aQueueWeight").text()){
        Component.Alert("warn", "权重未发生修改！");
        return;
    }
    initAbQueueWeight.RedisClusterA.WEIGHT = $("#aQueueWeight").text();
    initAbQueueWeight.RedisClusterB.WEIGHT = $("#bQueueWeight").text();

    var reqData = {};
    reqData.SERV_INST_ID = smsGwId;
    reqData.QUEUE_SERV_INST_ID = abQueveId;
    reqData.TOPO_JSON = initAbQueueWeight;
    $.ajax({
        url: Url.serverList.adjustSmsABQueueWeightInfo,
        asycn : false,
        type: "post",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify(reqData),
        success: function (result) {
            if (result.RET_CODE == 0) {
                Component.Alert("warn", "保存成功！");
            }else{
                Component.Alert("warn", result.RET_INFO);
            }
        }
    });
}
function addAWeight(){
    var aQueueWeight = Number($("#aQueueWeight").text());
    var bQueueWeight = Number($("#bQueueWeight").text());
    if(aQueueWeight<100){
      $("#aQueueWeight").text(aQueueWeight+1);
      $("#bQueueWeight").text(bQueueWeight-1);
      pro2.updateValue((Number)(aQueueWeight+1));
    }
}

function addBWeight(){
    var aQueueWeight =  Number($("#aQueueWeight").text());
    var bQueueWeight = Number($("#bQueueWeight").text());
    if(bQueueWeight<100){
      $("#aQueueWeight").text(aQueueWeight-1);
      $("#bQueueWeight").text(bQueueWeight+1);
      pro2.updateValue((Number)(aQueueWeight-1));
    }
}
//关闭刷新折线图数据定时器
function closeMmTimer() {
    window.clearInterval(mmTimer);
}

//拖拽进度条设置A/B Queue权重
function withdrawSmsABQueueWeight() {
    if (undefined == initAbQueueWeight || null == initAbQueueWeight.RedisClusterA.WEIGHT) {
        pro2.updateValue(0);
        $("#aQueueWeight").text(0);
        $("#bQueueWeight").text(100);
    } else {
        pro2.updateValue((Number)(initAbQueueWeight.RedisClusterA.WEIGHT));
        $("#aQueueWeight").text(initAbQueueWeight.RedisClusterA.WEIGHT);
        $("#bQueueWeight").text(initAbQueueWeight.RedisClusterB.WEIGHT);
    }
}

(function ($, echarts , window) {
    var JqMonitor = function (options) {
        this.options = options;
        this.instantaneousOpsPerSecA = [];//每秒的操作数
        this.usedCpuSysA = [];//内核态CPU%
        this.usedCpuUserA = [];//用户态CPU%
        this.userMemopyA = [];//使用内存
        this.maxMemopyA = [];//最大内存
        this.instantaneousOpsPerSecB = [];//每秒的操作数
        this.usedCpuSysB = [];//内核态CPU%
        this.usedCpuUserB = [];//用户态CPU%
        this.userMemopyB = [];//使用内存
        this.maxMemopyB = [];//最大内存    
        this.timeDataA = [];
        this.timeDataB = [];
        this.jvmEcharts;
        this.init();
    };

    // 时间转换 毫秒-年月日时分秒
    JqMonitor.prototype.setTimes = function (timer) {
        var time = new Date(timer);
        // var year = time.getFullYear();//年
        // var mon = time.getMonth() + 1;//0 
        // var day = time.getDate();//24
        var hour = time.getHours();//时
        var min = time.getMinutes();//分
        if (min < 10){
            min = "0" + min;
        }
        var sec = time.getSeconds();//秒
        if (sec < 10){
            sec = "0" + sec;
        }
        return hour + ':' + min + ':' + sec;
    }

    JqMonitor.prototype.post = function (data) {
        var self = this;
        Util.showLoading();
        $.ajax({
            url: Url.monitorList.getRedisHaServiceInfo,
            async: false,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    var dataAB = result.RET_INFO;
                    var instantaneousOpsPerSecA = [];
                    var usedCpuSysA = [];
                    var usedCpuUserA = [];
                    var userMemopyA = [];
                    var maxMemopyA = [];
                    var instantaneousOpsPerSecB = [];
                    var usedCpuSysB = [];
                    var usedCpuUserB = [];
                    var userMemopyB = [];
                    var maxMemopyB = [];
                    var timeDataA = [];
                    var timeDataB = [];
                    var dataListA = dataAB[data.INST_ID[0]];
                    if (dataListA.length > 0) {
                        for (var i = 0; i < dataListA.length; i++) {
                            instantaneousOpsPerSecA.push(dataListA[i].INSTANTANEOUS_OPS_PER_SEC);
                            usedCpuSysA.push(dataListA[i].USED_CPU_SYS);
                            usedCpuUserA.push(dataListA[i].USED_CPU_USER);
                            userMemopyA.push(dataListA[i].USED_MEMORY);
                            maxMemopyA.push(dataListA[i].MAXMEMORY);
                            timeDataA.push(self.setTimes(dataListA[i].TS));
                        }
                        self.instantaneousOpsPerSecA = instantaneousOpsPerSecA;
                        self.usedCpuSysA = usedCpuSysA;
                        self.usedCpuUserA = usedCpuUserA;
                        self.userMemopyA = userMemopyA;
                        self.maxMemopyA = maxMemopyA;
                        self.timeDataA = timeDataA;
                    }else{
                        self.instantaneousOpsPerSecA = instantaneousOpsPerSecA;
                        self.usedCpuSysA = usedCpuSysA;
                        self.usedCpuUserA = usedCpuUserA;
                        self.userMemopyA = userMemopyA;
                        self.maxMemopyA = maxMemopyA;
                        self.timeDataA = timeDataA;
                    }
                    var dataListB = dataAB[data.INST_ID[1]];
                    if (dataListB.length > 0) {
                        for (var i = 0; i < dataListB.length; i++) {
                            instantaneousOpsPerSecB.push(dataListB[i].INSTANTANEOUS_OPS_PER_SEC);
                            usedCpuSysB.push(dataListB[i].USED_CPU_SYS);
                            usedCpuUserB.push(dataListB[i].USED_CPU_USER);
                            userMemopyB.push(dataListB[i].USED_MEMORY);
                            maxMemopyB.push(dataListB[i].MAXMEMORY);
                            timeDataB.push(self.setTimes(dataListB[i].TS));
                        }
                        self.instantaneousOpsPerSecB = instantaneousOpsPerSecB;
                        self.usedCpuSysB = usedCpuSysB;
                        self.usedCpuUserB = usedCpuUserB;
                        self.userMemopyB = userMemopyB;
                        self.maxMemopyB = maxMemopyB;
                        self.timeDataB = timeDataB;
                    }else{
                        self.instantaneousOpsPerSecB = instantaneousOpsPerSecB;
                        self.usedCpuSysB = usedCpuSysB;
                        self.usedCpuUserB = usedCpuUserB;
                        self.userMemopyB = userMemopyB;
                        self.maxMemopyB = maxMemopyB;
                        self.timeDataB = timeDataB;
                    }
                    Util.hideLoading();
                }
            },
            error: function (error) {
                Util.hideLoading();
                Util.alert("error", error);
            }
        });
    }

    JqMonitor.prototype.init = function () {
        var self = this;
        self.jvmEcharts = echarts.init(document.getElementById('adjustWeightMain'));
        var date = new Date().getTime()
        self.options.END_TIMESTAMP = date;
        self.options.START_TIMESTAMP = date - self.options.TIME_INTERVAL;
        this.post(this.options);
        if(null == self.options.dataZoomStart){
            self.options.dataZoomStart = 75;
            self.options.dataZoomEnd = 100;
        }
        
        var option = {    
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'cross',
                    label: {
                        backgroundColor: '#6a7985'
                    }
                },
                formatter: function (params,ticket,callback) {
                    let str = params[0].name + "<br />";
                    params.forEach((item) => {
                        if(item.seriesIndex >=6 && item.seriesIndex <=9 ){
                            str +='<span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;left:5px;background-color:'
                            +item.color+'"></span>' + item.seriesName + " : " + (item.value/ 1024 / 1024).toFixed(2) + "MB<br />";
                        }else{
                            str +='<span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;left:5px;background-color:'
                            +item.color+'"></span>' + item.seriesName + " : " + item.value + "<br />";
                        }
                    
                    });
                    return str;
                }
            },      
            title: [
                {
                    subtext: 'QPS(A)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '4%',
                    top: '80'
                },
                {
                    subtext: 'QPS(B)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '54%',
                    top: '80'
                },
                {
                    subtext: 'CPU(A)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '4%',
                    top: '430'
                }, {
                    subtext: 'CPU(B)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '54%',
                    top: '430'
                }, {
                    subtext: 'MEM(A)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '4%',
                    top: '780'
                }, {
                    subtext: 'MEM(B)',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '54%',
                    top: '780'
                },
            ],
            color: ['#2b95f2','#2b95f2', '#45bb60', '#45bb60', "#efc73c", "#efc73c", "#38c5c1", "#38c5c1", "#825dd3", "#825dd3"],
            legend: [
                {
                    data: ['每秒操作数'],
                    left: '4%',
                    top: 60
                },
                {
                    data: ['每秒操作数'],
                    left: '54%',
                    top: 60
                },
                {
                    data: ['内核态CPU%', "用户态CPU%"],
                    left: '4%',
                    top: 410
                },
                {
                    data: ['内核态CPU%', "用户态CPU%"],
                    left: '54%',
                    top: 410
                },
                {
                    data: ['使用内存', "最大内存"],
                    left: '4%',
                    top: 760
                },
                {
                    data: ['使用内存', "最大内存"],
                    left: '54%',
                    top: 760
                }
            ],
            toolbox: {
                feature: {
                    dataZoom: {
                        yAxisIndex: 'none'
                    },
                    restore: {},
                    saveAsImage: {}
                },
                right: 20,
                top: 15
            },
            dataZoom: [
                {
                    show: true,
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                },
                {
                    show: true,
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '190',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5]
                }
            ],
            grid: [
                {
                    left: '8%',
                    top: '150',
                    width: "40%",
                    height: '200',
                }, {
                    left: '57%',
                    top: '150',
                    width: "40%",
                    height: '200',
                },{
                    left: "8%",
                    top: '500',
                    width: "40%",
                    height: '200',
                }, {
                    left: "57%",
                    top: '500',
                    width: "40%",
                    height: '200',
                },  {
                    left: "8%",
                    top: '850',
                    width: "40%",
                    height: '200',
                }, {
                    left: "57%",
                    top: '850',
                    width: "40%",
                    height: '200',
                },
            ],
            xAxis: [
                {
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataA
                },
                {
                    gridIndex: 1,
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataB,
                }, 
                {
                    gridIndex: 2,
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataA,
                },
                {
                    gridIndex: 3,
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataB,
                },
                {
                    gridIndex: 4,
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataA,
                },
                {
                    gridIndex: 5,
                    type: 'category',
                    boundaryGap: false,
                    axisLine: { 
                        onZero: true,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisLabel: {
                        rotate: 45,
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    data: this.timeDataB,
                }
            ],
            yAxis: [
                {
                    name: '次/秒',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                        
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        },
                        triggerTooltip: false
                    } 
                },
                {
                    gridIndex: 1,
                    name: '次/秒',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        },
                        triggerTooltip: false
                    } 
                    
                },
                {
                    gridIndex: 2,
                    name: '%',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        },
                        triggerTooltip: false
                    } 
                    
                },
                {
                    gridIndex: 3,
                    name: '%',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985'
                        },
                        triggerTooltip: false
                    } 
                    
                },
                {
                    gridIndex: 4,
                    name: '内存（≈MB）',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        formatter: function (v) {
                            var unit = v / 1024 / 1024;
                            return unit.toFixed(2)
                        },
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985',
                            formatter: function (params) {
                                var value = (params.value / 1024 / 1024).toFixed(2);
                                return value;
                            }
                        },
                        triggerTooltip: false
                    } 
                    
                },
                {
                    gridIndex: 5,
                    name: '内存（≈MB）',
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    axisLabel: {
                        formatter: function (v) {
                            var unit = v / 1024 / 1024;
                            return unit.toFixed(2)
                        },
                        textStyle: {
                            color: "#6E7079"
                        }
                    },
                    axisLine: {//y轴线的颜色以及宽度
                        show: false,
                        lineStyle: {
                            color: "#6E7079",
                        },
                    },
                    axisTick:{
                        show:false
                    },
                    splitLine: {//分割线配置
                        show:true,
                        lineStyle: {
                            color: "#E6E6E8",
                            type: "dashed"
                        }
                    },
                    axisPointer: {
                        show: true,
                        type: 'line',
                        lineStyle: {
                            type:'dashed'
                        },
                        label: {
                            backgroundColor: '#6a7985',
                            formatter: function (params) {
                                var value = (params.value / 1024 / 1024).toFixed(2);
                                return value;
                            }
                        },
                        triggerTooltip: false
                    } 
                }
            ],
            series: [
                {
                    name: '每秒操作数',
                    type: 'line',
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.instantaneousOpsPerSecA,
                },
                {
                    name: '每秒操作数',
                    type: 'line',
                    xAxisIndex: 1,
                    yAxisIndex: 1,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.instantaneousOpsPerSecB,
                },
                {
                    name: '内核态CPU%',
                    type: 'line',
                    xAxisIndex: 2,
                    yAxisIndex: 2,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.usedCpuSysA
                },
                {
                    name: '用户态CPU%',
                    type: 'line',
                    xAxisIndex: 2,
                    yAxisIndex: 2,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.usedCpuUserA
                },
                {
                    name: '内核态CPU%',
                    type: 'line',
                    xAxisIndex: 3,
                    yAxisIndex: 3,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.usedCpuSysB
                },
                {
                    name: '用户态CPU%',
                    type: 'line',
                    xAxisIndex: 3,
                    yAxisIndex: 3,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.usedCpuUserB
                },
                {
                    name: '使用内存',
                    type: 'line',
                    xAxisIndex: 4,
                    yAxisIndex: 4,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.userMemopyA 
                },
                {
                    name: '最大内存',
                    type: 'line',
                    xAxisIndex: 4,
                    yAxisIndex: 4,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.maxMemopyA 
                },
                {
                    name: '使用内存',
                    type: 'line',
                    xAxisIndex: 5,
                    yAxisIndex: 5,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.userMemopyB 
                },
                {
                    name: '最大内存',
                    type: 'line',
                    xAxisIndex: 5,
                    yAxisIndex: 5,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.maxMemopyB 
                }
            ]
        };

        self.jvmEcharts.setOption(option);
        self.jvmEcharts.on('datazoom',function(params){
            if(null != params.batch){
                self.options.dataZoomStart = params.batch[0].start;
                self.options.dataZoomEnd = params.batch[0].end;
            }else{
                self.options.dataZoomStart = params.start;
                self.options.dataZoomEnd = params.end;
            }
        })
    }
    
    window.JqMonitor = JqMonitor;
}(jQuery, echarts, window));

