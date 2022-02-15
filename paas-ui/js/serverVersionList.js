var serverVersionListParams = {};
var arrServVersion = {};

var select = $("#SERV_TYPE");
var addSelect =  $("#ADD_SERV_TYPE");
var addVersion = $("#SERV_VERSION");
var selectList = "";
var addSelectList = "";
var addVersionList = "";

function initPage(){
    serverType();
    loadServerVersionList();
}
function serverType(){
    $.ajax({
        url: rootUrl + "paas/metadata/getServTypeList",
        type: "post",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        success:function(result){
            if (result.RET_CODE == 0) {
                var data =result.SERV_TYPE;
                selectList+="<option selected >服务类别（默认所有）</option>";
                if(data.length>0){
                    for(var i=0; i<data.length; i++){
                        selectList+="<option value="+ data[i] +">"+ data[i] +"</option>";
                        addSelectList+="<option value="+ data[i] +">"+ data[i] +"</option>";
                    }
                    select.html(selectList);
                    addSelect.html(addSelectList);

                }   
            }
        }
    })
}

function loadServerVersionList() {  
    $('#server_version_list').mTable({
        url:         rootUrl + 'paas/metadata/getServTypeVerListByPage',
        countUrl:    rootUrl + 'paas/metadata/getServTypeVerCount',
        queryParams: serverVersionListParams,
        striped :    true,
        pagination : true,
        pageSize :   20,
        pageNumber : 1,
        columns : [{
            checkbox:true
        }, {
            field : "SERV_TYPE",
            title : "服务类别",
        }, {
            field : "VERSION",
            title : "版本",
        }]
    });
}

function showAddServerVersion() {
    $('#newServerVersion').modal("show");
    $(".modal-backdrop").appendTo($("#mainContent"));
}

function delServerVersion() {
    var servers = $('#server_version_list').mTable("getSelections");
    if (servers.length<1) {
        Util.alert("warn", "请选择服务版本");
        return;
    } else if (servers.length>1) {
        Util.alert("warn", "一次只能删除一个");
        return;
    }
    
    var data = {};
    data.SERV_TYPE = servers[0].SERV_TYPE;
    data.VERSION = servers[0].VERSION;
    layer.confirm("确认删除选择的服务版本吗？", {
        btn: ['是','否'],
        title: "确认"
    }, function(){
        layer.close(layer.index);
        $.ajax({
            url:         rootUrl + "paas/metadata/delCmptVersion",
            type:        "post",
            dataType:    "json",
            contentType: "application/json; charset=utf-8",
            data:        JSON.stringify(data),
            success: function(result) {
                if (result.RET_CODE == 0) {
                    layer.msg("删除成功");
                    $('#server_version_list').mTable("refresh");
                } else {
                    Util.alert("error", "删除服务版本失败！"+result.RET_INFO);
                }
            }
        });
    });
}

function saveServerVersion() {
    var data = {};
    data.SERV_TYPE   = $('#ADD_SERV_TYPE').find("option:selected").text();
    data.VERSION = $('#SERV_VERSION').val();

    $.ajax({
        url:         rootUrl + "paas/metadata/addCmptVersion",
        type :       "post",
        dataType:    "json",
        contentType: "application/json; charset=utf-8",
        data:        JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                $('#newServerVersion').modal("hide");
                $('#server_version_list').mTable("refresh");
            } else {
                Util.alert("error", "新增服务版本失败！" + result.RET_INFO);
            }
        }
    });
}

function searchServerVersion() {   
    serverVersionListParams.SERV_TYPE = $('#SERV_TYPE').val();
    if($('#SERV_TYPE').val() == "服务类别（默认所有）"){
        serverVersionListParams.SERV_TYPE = ""
    }
    $('#server_version_list').mTable("reload", {
        queryParams: serverVersionListParams
    });
}
