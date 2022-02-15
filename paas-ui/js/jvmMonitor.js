(function ($, echarts , window) {
    var JqMonitor = function (options) {
        this.options = options;
        this.dataList = [];
        this.memEdenUsed = [];
        this.memEdenCommitted = [];
        this.memSurvivorUsed = [];
        this.memSurvivorCommitted = [];
        this.memOldUsed = [];
        this.memOldCommitted = [];
        this.memPermUsed = [];
        this.memPermCommitted = [];
        this.memCodeUsed = []
        this.memCodeCommitted = [];
        this.memHeapUsed = [];
        this.memHeapCommitted = [];
        this.memNoheapUsed = [];
        this.memNoheapCommitted = [];
        this.gcYoungGcTime = [];
        this.gcFullGcTime = [];
        this.daemonThreadCount = [];
        this.threadCount = [];
        this.threadDealockedCount = [];
        this.timeData = [];
        this.jvmEcharts;
        this.init();
    };

    // 时间转换 毫秒-年月日时分秒
    JqMonitor.prototype.setTimes = function (timer) {
        var time = new Date(timer);
        var year = time.getFullYear();//年
        var mon = time.getMonth() + 1;//0 
        var day = time.getDate();//24
        var hour = time.getHours();//时
        var min = time.getMinutes();//分
        var sec = time.getSeconds();//秒
        return year + '/' + mon + '/' + day + ' ' + hour + ':' + min + ':' + sec;
    }

    JqMonitor.prototype.post = function (data) {
        var self = this;
        Util.showLoading();
        $.ajax({
            url: Url.monitorList.getJvmInfo,
            async: false,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    self.dataList = result.RET_INFO;
                    var memEdenUsed = [];
                    var memEdenCommitted = [];
                    var memSurvivorUsed = [];
                    var memSurvivorCommitted = [];
                    var memOldUsed = [];
                    var memOldCommitted = [];
                    var memPermUsed = [];
                    var memPermCommitted = [];
                    var memCodeUsed = [];
                    var memCodeCommitted = [];
                    var memHeapUsed = [];
                    var memHeapCommitted = [];
                    var memNoheapUsed = [];
                    var memNoheapCommitted = [];
                    var gcYoungGcTime = [];
                    var gcFullGcTime = [];
                    var daemonThreadCount = [];
                    var threadCount = [];
                    var threadDealockedCount = [];
                    var timeData = [];
                    if (self.dataList.length > 0) {
                        for (var i = 0; i < self.dataList.length; i++) {
                            memEdenUsed.push(self.dataList[i].MEM_EDEN_USED);
                            memEdenCommitted.push(self.dataList[i].MEM_EDEN_COMMITTED);
                            memSurvivorUsed.push(self.dataList[i].MEM_SURVIVOR_USED);
                            memSurvivorCommitted.push(self.dataList[i].MEM_SURVIVOR_COMMITTED);
                            memOldUsed.push(self.dataList[i].MEM_OLD_USED);
                            memOldCommitted.push(self.dataList[i].MEM_OLD_COMMITTED);
                            memPermUsed.push(self.dataList[i].MEM_PERM_USED);
                            memPermCommitted.push(self.dataList[i].MEM_PERM_COMMITTED);
                            memCodeUsed.push(self.dataList[i].MEM_CODE_USED);
                            memCodeCommitted.push(self.dataList[i].MEM_CODE_COMMITTED);
                            memHeapUsed.push(self.dataList[i].MEM_HEAP_USED);
                            memHeapCommitted.push(self.dataList[i].MEM_HEAP_COMMITTED);
                            memNoheapUsed.push(self.dataList[i].MEM_NOHEAP_USED);
                            memNoheapCommitted.push(self.dataList[i].MEM_NOHEAP_COMMITTED);
                            gcYoungGcTime.push(self.dataList[i].GC_YOUNG_GC_TIME);
                            gcFullGcTime.push(self.dataList[i].GC_FULL_GC_TIME);
                            daemonThreadCount.push(self.dataList[i].THREAD_DAEMON_THREAD_COUNT);
                            threadCount.push(self.dataList[i].THREAD_THREAD_COUNT);
                            threadDealockedCount.push(self.dataList[i].THREAD_DEADLOCKED_THREAD_COUNT);
                            timeData.push(self.setTimes(self.dataList[i].TS));
                        }
                        self.memEdenUsed = memEdenUsed;
                        self.memEdenCommitted = memEdenCommitted;
                        self.memSurvivorUsed = memSurvivorUsed;
                        self.memSurvivorCommitted = memSurvivorCommitted;
                        self.memOldUsed = memOldUsed;
                        self.memOldCommitted = memOldCommitted;
                        self.memPermUsed = memPermUsed;
                        self.memPermCommitted = memPermCommitted;
                        self.memCodeUsed = memCodeUsed;
                        self.memCodeCommitted = memCodeCommitted;
                        self.memHeapUsed = memHeapUsed;
                        self.memHeapCommitted = memHeapCommitted;
                        self.memNoheapUsed = memNoheapUsed;
                        self.memNoheapCommitted = memNoheapCommitted;
                        self.gcYoungGcTime = gcYoungGcTime;
                        self.gcFullGcTime = gcFullGcTime;
                        self.daemonThreadCount = daemonThreadCount;
                        self.threadCount = threadCount;
                        self.threadDealockedCount = threadDealockedCount;
                        self.timeData = timeData;
                    }else{
                        self.memEdenUsed = memEdenUsed;
                        self.memEdenCommitted = memEdenCommitted;
                        self.memSurvivorUsed = memSurvivorUsed;
                        self.memSurvivorCommitted = memSurvivorCommitted;
                        self.memOldUsed = memOldUsed;
                        self.memOldCommitted = memOldCommitted;
                        self.memPermUsed = memPermUsed;
                        self.memPermCommitted = memPermCommitted;
                        self.memCodeUsed = memCodeUsed;
                        self.memCodeCommitted = memCodeCommitted;
                        self.memHeapUsed = memHeapUsed;
                        self.memHeapCommitted = memHeapCommitted;
                        self.memNoheapUsed = memNoheapUsed;
                        self.memNoheapCommitted = memNoheapCommitted;
                        self.gcYoungGcTime = gcYoungGcTime;
                        self.gcFullGcTime = gcFullGcTime;
                        self.daemonThreadCount = daemonThreadCount;
                        self.threadCount = threadCount;
                        self.threadDealockedCount = threadDealockedCount;
                        self.timeData = timeData;
                    }
                    Util.hideLoading();
                }
            },
            error: function (error) {
                Util.alert("error", error);
            }
        });
    }

    JqMonitor.prototype.init = function () {
        var self = this;
        self.jvmEcharts = echarts.init(document.getElementById('main'));
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
                        if(item.seriesIndex >=0 && item.seriesIndex <=13 ){
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
                    // text: 'Memory',
                    subtext: 'Eden',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    // itemGap: 40,
                    left: '4%',
                    top: '80'
                },
                {
                    subtext: 'Survivor',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '28%',
                    top: '80'
                },
                {
                    subtext: 'Old',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '54%',
                    top: '80'
                },
                {
                    subtext: 'Perm',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '78%',
                    top: '80'
                },
                {
                    subtext: 'Code',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '4%',
                    top: '480'
                },
                {
                    subtext: 'Heap',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '29%',
                    top: '480'
                }, {
                    subtext: 'Noheap',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '54%',
                    top: '480'
                }, {
                    // text: 'GC',
                    subtext: 'YangGC',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    // itemGap: 20,
                    left: '4%',
                    top: '930'
                }, {
                    subtext: 'FullGC',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '29%',
                    top: '930'
                }, {
                    // text: 'Thread',
                    subtext: 'Daeam',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    // itemGap: 20,
                    left: '4%',
                    top: '1380'
                }, {
                    subtext: 'ThreadCount',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '28%',
                    top: '1380'
                }, {
                    subtext: 'Deadlack',
                    subtextStyle: {
                        fontWeight: "bold",
                        fontSize: 14,
                    },
                    left: '53%',
                    top: '1380'
                },
            ],
            color: ['#2b95f2', '#45bb60', "#efc73c", "#38c5c1"],
            legend: [
                {
                    data: ['Used', 'Committed', 'GcTime', "Count"],
                    left: 0,
                    top: 15
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
                right: 200,
                top: 15
            },
            // axisPointer: {
            //     link: { xAxisIndex: 'all' }
            // },
            dataZoom: [
                {
                    show: true,
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    show: true,
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                },
                {
                    type: 'inside',
                    realtime: true,
                    start: self.options.dataZoomStart,
                    end: self.options.dataZoomEnd,
                    top: 15,
                    right: '320',
                    width: 200,
                    xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                }
            ],
            grid: [
                {
                    left: '5%',
                    top: '150',
                    width: "20%",
                    height: '200',
                }, {
                    left: '30%',
                    top: '150',
                    width: "20%",
                    height: '200',
                }, {
                    left: "55%",
                    top: '150',
                    width: "20%",
                    height: '200',
                }, {
                    left: '79%',
                    top: '150',
                    width: "20%",
                    height: '200',
                }, {
                    left: '5%',
                    top: '550',
                    width: "20%",
                    height: '200',
                }, {
                    left: "30%",
                    top: '550',
                    width: "20%",
                    height: '200',
                }, {
                    left: "55%",
                    top: '550',
                    width: "20%",
                    height: '200',
                }, {
                    left: "5%",
                    top: '1000',
                    width: "20%",
                    height: '200',
                }, {
                    left: "30%",
                    top: '1000',
                    width: "20%",
                    height: '200',
                }, {
                    left: "5%",
                    top: '1450',
                    width: "20%",
                    height: '200',
                }, {
                    left: "30%",
                    top: '1450',
                    width: "20%",
                    height: '200',
                }, {
                    left: "55%",
                    top: '1450',
                    width: "20%",
                    height: '200',
                }
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
                    data: this.timeData
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
                    data: this.timeData,
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
                    data: this.timeData,
                }, {
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
                    data: this.timeData,
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
                    data: this.timeData,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 6,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 7,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 8,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 9,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 10,
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
                    data: this.timeData,
                },
                {
                    gridIndex: 11,
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
                    data: this.timeData,
                }
            ],
            yAxis: [
                {
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 1,
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 2,
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 3,
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 4,
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 6,
                    name: '内存（≈MB）',
                    type: 'value',
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
                    gridIndex: 7,
                    name: '时间（ms）',
                    type: 'value',
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
                    gridIndex: 8,
                    name: '时间（ms）',
                    type: 'value',
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
                    gridIndex: 9,
                    name: '次数（次）',
                    type: 'value',
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
                    gridIndex: 10,
                    name: '次数（次）',
                    type: 'value',
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
                    gridIndex: 11,
                    name: '次数（次）',
                    type: 'value',
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
                }
            ],
            series: [
                {
                    name: 'Used',
                    type: 'line',
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memEdenUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memEdenCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 1,
                    yAxisIndex: 1,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memSurvivorUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 1,
                    yAxisIndex: 1,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memSurvivorCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 2,
                    yAxisIndex: 2,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memOldUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 2,
                    yAxisIndex: 2,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memOldCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 3,
                    yAxisIndex: 3,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memPermUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 3,
                    yAxisIndex: 3,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memPermCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 4,
                    yAxisIndex: 4,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memCodeUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 4,
                    yAxisIndex: 4,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memCodeCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 5,
                    yAxisIndex: 5,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memHeapUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 5,
                    yAxisIndex: 5,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memHeapCommitted
                },
                {
                    name: 'Used',
                    type: 'line',
                    xAxisIndex: 6,
                    yAxisIndex: 6,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memNoheapUsed
                },
                {
                    name: 'Committed',
                    type: 'line',
                    xAxisIndex: 6,
                    yAxisIndex: 6,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.memNoheapCommitted
                },
                {
                    name: 'GcTime',
                    type: 'line',
                    xAxisIndex: 7,
                    yAxisIndex: 7,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.gcYoungGcTime
                },
                {
                    name: 'GcTime',
                    type: 'line',
                    xAxisIndex: 8,
                    yAxisIndex: 8,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.gcFullGcTime
                },
                {
                    name: 'Count',
                    type: 'line',
                    xAxisIndex: 9,
                    yAxisIndex: 9,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.daemonThreadCount
                },
                {
                    name: 'Count',
                    type: 'line',
                    xAxisIndex: 10,
                    yAxisIndex: 10,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.threadCount
                },
                {
                    name: 'Count',
                    type: 'line',
                    xAxisIndex: 11,
                    yAxisIndex: 11,
                    symbolSize: 5,
                    hoverAnimation: false,
                    data: this.threadDealockedCount
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

