var row_data;

function loadSSHList(ip, name) {
    $('#server_ip').text(ip);
    $('#server_name').text(name);
    
    $('#ssh_list').mTable({
        url:         rootUrl + 'paas/metadata/getSSHListByIP',
        countUrl:    rootUrl + 'paas/metadata/getSSHCountByIP',
        queryParams: {SERVER_IP: ip},
        striped :    true,
        pagination : true,
        pageSize :   20,
        pageNumber : 1,
        columns : [{
            checkbox:true
        }, {
            field : "SSH_NAME",
            title : "用户名",
        }, {
            field : "SERV_CLAZZ",
            title : "服务类型",
        }, {
            field : "SERVER_IP",
            title : "服务器IP",
        }, {
            field : "SSH_PORT",
            title : "SSH端口",
        }, {
            title : "操作",
            isButtonColumn:true,
            buttons:[{
                text:"修改",
                onClick:function(_button, row, _index){
                    row_data = row;
                    $('#modSSHHeader').text("修改SSH用户("+$('#server_ip').text()+")");
                    $('#M_SSH_NAME').val(row.SSH_NAME);
                    $('#M_SSH_PWD').val('');
                    $('#M_SSH_PORT').val(row.SSH_PORT);
                    $('#modSSH').modal("show");
                    $(".modal-backdrop").appendTo($("#mainContent"));
                }
            }]
        }]
    });
}

function showAddSSH() {
    $('#newSSHHeader').text("新增SSH用户("+$('#server_ip').text()+")");
    Util.clearForm("newSSHForm");
    $('#SSH_NAME').prop("disabled", false);
    $('#SSH_PORT').val("22");
    $('#newSSH').modal("show");
    $(".modal-backdrop").appendTo($("#mainContent"));
}

function addSSH() {
    var loading = $('#loadingDiv');
    var type = "";
    $('input[name="SERV_CLAZZ"]:checked').each( function() {  
          type += "," + $(this).val();
    });
    
    var data = Util.getFormParam("newSSHForm");
    data.SERVER_IP   = $('#server_ip').text();
    data.SERVER_NAME = $('#server_name').text();
    data.SSH_PORT    = parseInt($('#SSH_PORT').val());

    $.ajax({
        url:         rootUrl + "paas/metadata/addSSH",
        type :       "post",
        dataType:    "json",
        contentType: "application/json; charset=utf-8",
        data:        JSON.stringify(data),
        success: function(result) {
            if (result.RET_CODE == 0) {
                $('#newSSH').modal("hide");
                $('#ssh_list').mTable("refresh");
            } else {
                Util.alert("error", "新增SSH用户失败！" + result.RET_INFO);
            }
        }
    });
}

function modSSH() {
    var postData = {};
    var loading  = $('#loadingDiv');
    var new_name = $('#M_SSH_NAME').val();
    var new_pwd  = $('#M_SSH_PWD').val();
    var new_port = parseInt($('#M_SSH_PORT').val());
    postData.SSH_NAME  = new_name;
    postData.SSH_PWD   = new_pwd;
    postData.SSH_PORT  = new_port;
    postData.SSH_ID    = row_data.SSH_ID;
    postData.SERVER_IP = row_data.SERVER_IP;

    if (new_name == '') {
        Util.alert("info", "用户名不能为空！");
        return;
    }

    if (new_pwd == '') {
        Util.alert("info", "密码不能为空！");
        return;
    }

    // if (new_name == row_data.SSH_NAME 
    //     && new_pwd == row_data.SSH_PWD) {
        
    //     Util.alert("info", "SSH信息未修改无需保存！");
    //     return;
    // }

    $.ajax({
        url:         rootUrl + "paas/metadata/modSSH",
        type :       "post",
        dataType:    "json",
        contentType: "application/json; charset=utf-8",
        data:        JSON.stringify(postData),
        success: function(result) {
            if (result.RET_CODE == 0) {
                $('#modSSH').modal("hide");
                $('#ssh_list').mTable("refresh");
            } else {
                Util.alert("error", "新增SSH用户失败！" + result.RET_INFO);
            }
        }
    });
}

function delSSH() {
    var sshs = $('#ssh_list').mTable("getSelections");
    if (sshs.length < 1) {
        Util.alert("warn", "请选择SSH用户");
        return;
    }

    if (sshs.length > 1) {
        Util.alert("warn", "一次只能删除一条数据");
        return;
    }
    
    var ssh = $("#ssh_list").mTable("getSelections");
    var postData = {};
    postData.SSH_ID    = ssh[0].SSH_ID;
    postData.SERVER_IP = ssh[0].SERVER_IP;
    
    layer.confirm("确认删除选择的SSH用户吗？", {
        btn: ['是','否'],
        title: "确认"
    }, function(){
        layer.close(layer.index);
        var loading = $('#loadingDiv');
        
        $.ajax({
            url:         rootUrl + "paas/metadata/delSSH",
            type :       "post",
            dataType:    "json",
            contentType: "application/json; charset=utf-8",
            data:        JSON.stringify(postData),
            success: function(result) {
                if (result.RET_CODE == 0) {
                    layer.msg("删除成功");
                    $('#ssh_list').mTable("refresh");
                } else {
                    Util.alert("error", "删除SSH用户失败！"+result.RET_INFO);
                }
            }
        });
    });
}
