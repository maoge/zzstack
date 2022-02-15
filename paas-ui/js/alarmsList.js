var alarmListParams = {};
var $alarmList = $("#alarms_list");

(function ($, window) {

    var AlarmsList = function (options) {
        this.options = options;
        this.init();
    };

    AlarmsList.prototype.init = function() {
        var tableEle = this.options.TABLE_ELE,
            that     = this;
        this.mTable  = tableEle.mTable({
            url: Url.alarm.getAlarmList,
            countUrl: Url.alarm.getAlarmCount,
            striped : true,
            pagination : true,
            pageSize : 20,
            pageNumber : 1,
            columns : [{
                field : "SERV_INST_ID",
                title : "服务ID",
            },{
                field : "SERV_TYPE",
                title : "服务类别",
            },{
                field : "SERV_NAME",
                title : "服务名",
            },{
                field : "INST_ID",
                title : "实例ID",
            },{
                field : "CMPT_NAME",
                title : "组件名称",
            }, {
                field : "ALARM_INFO",
                title : "告警信息",
            }, {
                field : "ALARM_TIME",
                title : "告警时间",
            }, {
                field : "IS_DEALED",
                title : "处理标志",
                format:function(value, _row, _index){
                    return value=='1' ? '是' : '否';
                }
            }, {
                field : "DEAL_TIME",
                title : "处理时间",
            }, {
                field : "DEAL_ACC_NAME",
                title : "处理人",
            }, {
                title : "操作",
                isButtonColumn:true,
                buttons:[
                    {
                        text:"清除",
                        format:function(_value, row){
                            if(row.IS_DEALED == "1") {
                                return { hided:true };
                            }
                        },
                        onClick:function(button,row,index){
                            that.clearAlarm(row);
                        }
                    },
                    {
                        text:"管理",
                        onClick:function(_button, row, _index) {
                            $mainContainer.load("serviceManage.html",function() {
                                init(row.SERV_INST_ID, row.SERV_NAME, row.SERV_TYPE, row.SERV_CLAZZ, row.IS_PRODUCT, row.VERSION);
                            });
                        }
                    }
                ]
            }]
        });
    }

    AlarmsList.prototype.clearAlarm = function (row) {
        var data = {};
            data.ALARM_ID       = row.ALARM_ID;
            data.INST_ID        = row.INST_ID;
            data.ALARM_TYPE     = row.ALARM_TYPE;
            data.DEAL_ACC_NAME  = $.cookie("ACC_NAME");
            that = this;
        
        var loading = $('#loadingDiv');

        $.ajax({
            url:  Url.alarm.clearAlarm,
            type : "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data),
            success: function(result) {
                if (result.RET_CODE == 0) {
                    that.options.TABLE_ELE.mTable("refresh");
                } else {
                    Util.alert("error", "清除失败");
                }
            }
        });
    }

    window.AlarmsList = AlarmsList;

}(jQuery, window));

function searchAlarm() {
    alarmListParams.SERV_INST_ID = $('#S_SERV_INST_ID').val();
    alarmListParams.INST_ID = $('#S_INST_ID').val();
    alarmListParams.DEAL_FLAG = $('#S_DEAL_FLAG').val();
    $alarmList.mTable("reload", {
        queryParams: alarmListParams,
        pageNumber: 1
    });
}
