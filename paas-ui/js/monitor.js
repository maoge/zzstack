/**
 * Created by guozh on 2018/1/8.
 */
Date.prototype.simpleFormat = function (fmt) {
    var o = {
        "Y+":this.getFullYear(),
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S": this.getMilliseconds()
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
};

(function ($,echarts) {
    var MChart = function(ele,options){
        this.options = options;
        this.$ele = $(ele);
        this.$eles = {};
        this.eleOptionsData={};
        this.data = [];
        this.echart = {};
        this.levels = {};
        this.init();
    };
    MChart.DEFAULT = {
        url : "http://127.0.0.1:9991/collectdata/getHisCollectData",
        instId : "",
        theme : "dark",
        endTS : (new Date()).getTime(),
        startTS : (new Date()).getTime() - 1000*60*60*5,
        dataZoomStart : 0,
        dataZoomEnd : 100,
        interval : 10,
        config:[]
    };
    MChart.ECHART_OPTIONS = {
        tooltip: {//全局提示框组件
            trigger: 'axis',//坐标轴触发
            position: function (pt) {
                return [pt[0], '40%'];//显示位置
            }
        },
        title: {
            left: 'center',
            text: '监控',
        },
        toolbox: {
            feature: {//工具配置项
                restore: {},//配置项还原。
                saveAsImage: {}//保存为图片。
            }
        },
        xAxis: {
            type: 'category',
            boundaryGap: [1,'100%']
        },
        yAxis : {},
        series: [
        ]
    };
    MChart.ECHART_OPTIONS_SERIES = {
        name:"监控",
        type:"line",
        smooth:true,//是否平滑曲线显示
        /*symbol: 'none',//标记的图形。*/
        sampling: 'average',//折线图在数据量远大于像素点时候的降采样策略 取过滤点的平均值
        itemStyle: {
            normal: {
                color: 'rgb(255, 70, 131)'//
            }
        },
        areaStyle: {
            normal: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                    offset: 0,
                    color: 'rgb(255, 158, 68)'
                }, {
                    offset: 1,
                    color: 'rgb(255, 70, 131)'
                }])
            }
        }
    }
    MChart.prototype.init = function(){
        var conf = this.options.config;
        //全部初始化echars
        for(var i in conf){
            var row = conf[i],
                container = row['container'],
                param = row['param'],
                echartOption = row['echartOption'],
                title = row['echartOption']['title']['text'],
                //与固定的合在一起
                echartOptions = $.extend(true,{},MChart.ECHART_OPTIONS,echartOption),
                echartOptionsSeries = echartOptions['series'],
                seriesesLastVer = [],
                paramReplaceDone = param.replace('.',''),
                id = $('#' + container),
                char = echarts.init(id[0],this.options.theme);

            if(row['echartOption']['series']){
                var serieses = row['echartOption']['series'];
                for(var j in serieses){
                    var seri = serieses[j];
                    seri = $.extend(true,{},MChart.ECHART_OPTIONS_SERIES,seri);
                    seriesesLastVer.push(seri);
                }
            }else{
                var seri = $.extend(true,{},MChart.ECHART_OPTIONS_SERIES,{name:title});
                seriesesLastVer.push(seri);
            }
            echartOptions['series'] = seriesesLastVer;

            char.setOption(echartOptions);
            char.setOption(this.getChartOpiton(title));
            char.showLoading();
            this.$eles[paramReplaceDone] = char;
            this.levels[paramReplaceDone] = 1;
        }
        this.post();
    };
    MChart.prototype.post = function(){
        var that = this;
        var request = {
            type : "post",
            data : {"INST_ID" : that.options.instId , "START_TS" : that.options.startTS, "END_TS":that.options.endTS},
            url: this.options.url,
            success :function(res){
                if(res['RET_CODE'] == 0){
                    var data = res['RET_INFO'];
                    that.data = data;
                    that.updateDateToEchart();
                    if(that.options.interval){
                        that.setInterval();
                    }
                }else{
                    alert("error ,please check the console command!")
                }
                that.hideLoading();
            },
            error : function(res){
                console.error(res);
                that.hideLoading();
            }
        };
        $.ajax(request);
    };
    MChart.prototype.updateDateToEchart = function(){
        var data = this.data,
            conf = this.options.config,
            that = this;
        for(var i in conf){
            var row = conf[i],
                param = row['param'],
                paramReplaceDone = param.replace('.',''),
                title = row['echartOption']['title']['text'],
                char = this.$eles[paramReplaceDone];
            char['paramReplaceDone'] = paramReplaceDone;
            //设置数据到Echarts里面
            this.setChartData(char,title,param,paramReplaceDone);
            //开启echart的dataZoom的事件
            char.on("dataZoom",function(params){
                var zoomLevel,paramReplaceDoneCp = this['paramReplaceDone'];
                if(params['start'] == 0 || params['start']){
                    zoomLevel = getTimeLevel(that.options.startTS, that.options.endTS, (params.end -params.start)/100);
                }else{
                    var zoomInfo = params['batch']['0'];
                    zoomLevel = getTimeLevel(that.options.startTS, that.options.endTS, (zoomInfo.end -zoomInfo.start)/100);
                }
                if(zoomLevel != that.levels[paramReplaceDoneCp]){
                    that.updateXAxisDataByLevel(this,zoomLevel);
                    that.levels[paramReplaceDoneCp] = zoomLevel;
                }
            });
        }

    }
    MChart.prototype.setChartData = function(char,title,param,paramReplaceDone){
        var data = this.data,
            len = data.length,
            xAxisData = [],
            echartData = [],
            zoomLevel = getTimeLevel(this.options.startTS, this.options.endTS, (this.options.dataZoomEnd - this.options.dataZoomStart)/100);
        this.levels[paramReplaceDone] = zoomLevel;
        for(var row in data){
            var ts = data[row]['TS'],
                paramUsed =data[row][param];
            xAxisData.push(getTimeByLevel(zoomLevel,ts));
            echartData.push(paramUsed);
        }
        var res = {
            xAxis: {
                data: xAxisData
            },
            series: [{
                name:title,
                data: echartData
            }]
        }
        this.eleOptionsData[paramReplaceDone] = res;
        char.setOption(res);
    };
    MChart.prototype.getChartOpiton = function(title,echartType){
        var that = this;
        return {
            tooltip : {
                formatter : function(params){
                    var html =
                        getTimeByLevel(1,that.data[params[0]['dataIndex']]['TS']) + "</br>" +
                        '<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:rgb(255, 100, 151);"></span>'+
                        params[0]['data'];
                    return html;
                }
            },
            dataZoom: [{
                type: 'inside',
                start: this.options.dataZoomStart,
                end: this.options.dataZoomEnd
            }, {
                start: this.options.startTS,
                end: this.options.endTS,
                handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
                handleSize: '80%',
                handleStyle: {
                    color: '#fff',
                    shadowBlur: 3,
                    shadowColor: 'rgba(0, 0, 0, 0.6)',
                    shadowOffsetX: 2,
                    shadowOffsetY: 2
                }
            }]
        };
    };
    MChart.prototype.setInterval = function(){
        var that = this,
            startTS = that.options.endTS;
        this.intervalEvent = setInterval(function(){
            var endTS = (new Date()).getTime(),
                request = {
                    type : "post",
                    data : {"INST_ID" : that.options.instId , "START_TS" : startTS, "END_TS":endTS},
                    url: that.options.url,
                    success :function(res){
                        if(res['RET_CODE'] == 0){
                            var data = res['RET_INFO'];
                            if(data.length > 0){
                                that.appendChartData(data);
                                that.options.endTS = endTS;
                            }
                        }else{
                            alert("error ,please check the console command!")
                        }
                    },
                    error : function(res){
                        console.error(res);
                    }
                };
            $.ajax(request);
        },this.options.interval * 1000);
    };
    MChart.prototype.appendChartData = function(data){
        var len = data.length,
            conf = this.options.config,
            that = this;
        for(var i in data){
            var ts = data[i]['TS'];
            for(var j in conf){
                var row = conf[j],
                    param = row['param'],
                    paramReplaceDone = param.replace('.',''),
                    char = this.$eles[paramReplaceDone],
                    paramUsed = data[i][param],
                    xAxisData = this.eleOptionsData[paramReplaceDone]['xAxis']['data'],
                    echartData = this.eleOptionsData[paramReplaceDone]['series'][0]['data'];
                xAxisData.push(getTimeByLevel(this.levels[paramReplaceDone],ts));
                echartData.push(paramUsed);
                char.setOption({
                    xAxis: {
                        data: xAxisData
                    },
                    series: [{
                        data: echartData
                    }]
                });
            }

        }
        that.data = that.data.concat(data);
    };
    MChart.prototype.hideLoading = function(){
        var conf = this.options.config;
        for(var i in conf) {
            var row = conf[i],
                param = row['param'],
                paramReplaceDone = param.replace('.', ''),
                char = this.$eles[paramReplaceDone];
            char.hideLoading();
        }
    };
    MChart.prototype.reloadByTime = function(options){
        this.options.startTS = options['startTS'];
        this.options.endTS   = options['endTS'];
        if(options['interval'] == 0 ||  options['interval']){
            this.options.interval = options['interval'];
        }
        clearInterval(this.intervalEvent);
        this.init();
    };
    MChart.prototype.updateXAxisDataByLevel = function(char,level){
        var xAxisData = [];
        for(var row in this.data){
            var ts = this.data[row]['TS'];
            xAxisData.push(getTimeByLevel(level,ts));
        }
        char.setOption({
            xAxis: {
                data: xAxisData
            }
        });
    };

    //计算X轴显示的级别
    function getTimeLevel(startTS,endTS, percent){
        var interval = (parseFloat(endTS) - parseFloat(startTS)) / 1000;
        percent && (interval = interval * percent);
        if(interval <= 30 * 60){// hh:mi:ss
            return 1;
        }else if(30 * 60< interval <= 24 * 60 * 60 ){ //hh:mi
            return 2;
        }else if(interval > 24 * 60 * 60) {// mm/dd hh:mi
            return 3;
        }
    }
    function getTimeByLevel(level,ts){
        var time = new Date(parseFloat(ts));
        switch (level){
            case 1 :// hh:mi:ss
                return time.simpleFormat("hh:mm:ss");
                break;
            case 2 ://hh:mi
                return time.simpleFormat("hh:mm");
                break;
            case 3 :// mm/dd hh:mi
                return time.simpleFormat("mm-dd hh:mm");
                break;
            default :
                return "";
                break;
        }
    }
    var allowedMethods = ["reloadByTime"];
    $.fn.mChart = function(option){
        var $this = $(this);
        var args = Array.prototype.slice.call(arguments, 1),//取出option后面的参数
            value,//函数执行的返回值
            data = $this.data('jquery.mchart'),
            options = $.extend({},MChart.DEFAULT,$this.data,typeof option === 'object' && option);
        if(typeof option ==='string') {  //如果传进来是string类型，当成mTable函数执行
            if($.inArray(option, allowedMethods) < 0){
                throw new Error("Unknown method: " + option);
            }
            if (!data) {
                return;
            }
            value = data[option].apply(data, args);
        }
        if(!data){ //如果没有数据，又不是string类型，就进行初始化
            $this.data('jquery.mchart',(data = new MChart($this,options)));
        }
        return value === 'undefined' ? this : value; //如果是执行mTable函数就返回value，否则返回mTable对象本身
     /*
        var value;
        if(typeof option == "string"){
            return data[option].apply(data, args);
        }else if(typeof option == "object"){
            var options = {},
                $this = $(this);
            options = $.extend({},MChart.DEFAULT,option);
            value =
        }

        $this.data('jquery.mtable',(data = new MTable(this,options)));
        return new MChart($this,options);*/
    };
    $.fn.mChart.Constructor = MChart;
}(jQuery,echarts));