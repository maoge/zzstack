(function ($) {
    var popupForm = function (eleType, options) {
        this.options = options;
        this.show = function (data,type) {
            this.$popup || this.createPopupForm(options,type);
            if("edit" == type){
                this.title = "Edit " + eleType;
                this.$closeBtn.hide();
                this.$cancelBtn.show();
                this.$submitBtn.show();
                var inputForm = this.$form.find('input');
                for(var i=0; i<inputForm.length; i++){
                    if("INST_ID" == inputForm[i].name || "SERV_INST_ID" == inputForm[i].name || "DG_NAME" == inputForm[i].name || 'SERV_CONTAINER_NAME'== inputForm[i].name){
                        continue;
                    }
                    inputForm[i].removeAttribute('disabled');
                }
                var selectForm = this.$form.find('select');
                for(var i=0; i<selectForm.length; i++){
                    selectForm[i].removeAttribute('disabled');
                }
                var textareaForm = this.$form.find('textarea');
                for(var i=0; i<textareaForm.length; i++){
                    textareaForm[i].removeAttribute('disabled');
                }
            }else if("view" == type){
                this.title = "View " + eleType;
                this.$closeBtn.show();
                this.$cancelBtn.hide();
                this.$submitBtn.hide();
                var inputForm = this.$form.find('input');
                for(var i=0; i<inputForm.length; i++){
                    inputForm[i].setAttribute('disabled', 'disabled');
                }
                var selectForm = this.$form.find('select');
                for(var i=0; i<selectForm.length; i++){
                    selectForm[i].setAttribute('disabled', 'disabled');
                }
                var textareaForm = this.$form.find('textarea');
                for(var i=0; i<textareaForm.length; i++){
                    textareaForm[i].setAttribute('disabled', 'disabled');
                }
            }else{
                this.title = "Add " + eleType;
                this.$closeBtn.hide();
                this.$cancelBtn.show();
                this.$submitBtn.show();
                var inputForm = this.$form.find('input');
                for(var i=0; i<inputForm.length; i++){
                    if("INST_ID" == inputForm[i].name || "SERV_INST_ID" == inputForm[i].name || "DG_NAME" == inputForm[i].name){
                        continue;
                    }
                    inputForm[i].removeAttribute('disabled');
                }
                var selectForm = this.$form.find('select');
                for(var i=0; i<selectForm.length; i++){
                    selectForm[i].removeAttribute('disabled');
                }
                var textareaForm = this.$form.find('textarea');
                for(var i=0; i<textareaForm.length; i++){
                    textareaForm[i].removeAttribute('disabled');
                }
            }
            this.putData(data);
            this.$popupTitle.find("div").text(this.title);
            this.$popup.show();
            this.resize();
        };
        this.formToJson = function () {
            var data = [], that = this, len = this.options.data.length, isAppend = true;
            $.each(this.options.data, function (index, row) {
                for (var id in row) {
                    var type = row[id]["type"];
                    if (id === "OS_PWD") {
                        data.push("\"OS_PWD\":\"" + that.getPwdByUser() + "\"");
                    } else if (type === "string") {
                        data.push("\"" + id + "\":" + "\"" + that.$form.find(":input[name='" + id + "']").val() + "\"");
                    } else if (type === "array" || type === "object") {
                        continue;
                    } else {
                        data.push("\"" + id + "\":" + that.$form.find(":input[name='" + id + "']").val());
                    }
                }
            });
            return "{" + data.toString() + "}";
        };
        this.putData = function (data) {
            if (data == null) {
                return;
            }
            for (var key in data) {
                this.$form.find(":input[name='" + key + "']").val(data[key]);
                if (key === "SSH_ID") {
                    this.getUsersByIP();
                }
            }
        };
        this.hide = function () {
            this.$popup && this.$popup.hide();
        };
        this.clearLoading = function () {
            this.$popupBody.css("z-index", 1000);
            this.$loading.hide();
        };
        this.startLoading = function () {
            this.$popupBody.css("z-index", 998);
            this.$loading.show();
        };
        this.resize = function () {//会随浏览器大小自动居中和调整popup的大小
            this.$popupBody.css({
                "margin-left": -(this.$popupBody.width() / 2) + "px",
                "margin-top": -(this.$popupBody.height() / 2) + "px"
            });//居中效果
            var padding = this.$popupContent.css("padding-top").split("px")[0];
            this.$popupBody.height(this.$popupTitle.height() + this.contentHeight + this.$popupFoot.height() + 3);//3是border的高度
            this.$popupBody.width(this.contentWidth);
            this.$popupContent.height(this.$popupBody.height() - this.$popupTitle.height() - this.$popupFoot.height() - padding * 2);//30是2 * padding的高度
        };
        this.setMasterSelect = function (options) {
            var masterSelect = this.$form.find("select[name='MASTER_ID']");
            masterSelect.html(options);
        };
        this.createPopupForm = function (options,type) {
            var that = this;
            this.createForm(options,type),
                this.$popup = $("<div class='popup'></div>"),
                this.$popupShelter = $("<div class='popupShelter'></div>"),
                this.$popupBody = $("<div class='popupBody'></div>"),
                this.$popupTitle = $("<div class='popupBodyTitle'><div>这是一个Title！</div></div>"),
                this.$popupClose = $("<span class='popupBodyClose'>x</span>"),
                this.$popupContent = $("<div class = 'popupBodyContent'></div>"),
                this.$popupFoot = $("<div class = 'popupBodyFoot'></div>"),
                this.$closeBtn = $("<button id = 'closePopupFormButton' type = 'button' class = 'popupBodyFootBtn' >关闭</button>"),
                this.$cancelBtn = $("<button id = 'cancelPopupFormButton' type = 'button' class = 'popupBodyFootBtn' >取消</button>"),
                this.$submitBtn = $("<button id = 'submitPopupFormButton' type = 'button' class = 'popupBodyFootBtn' >提交修改</button>"),
                this.$loading = $("<div class='popupLoading' style='display: none'></div>");

            this.$popupContent.append(this.$form);
            this.$popupFoot.append(this.$closeBtn, this.$submitBtn, this.$cancelBtn);
            this.$popupBody.append(this.$popupTitle, this.$popupClose, this.$popupContent, this.$popupFoot);
            this.$popup.append(this.$popupShelter, this.$popupBody);
            this.$popup.appendTo($('body'));
            this.$loading.appendTo($('body'));

            this.contentHeight = this.$popupContent.height();
            this.contentWidth = this.$popupContent.width();
            //绑定事件
            $(window).resize(function () {
                that.resize();
            });
            this.$popupFoot.append();
            this.$closeBtn.click(function () {
                that.hide();
                that.options.cancelCallBack();
            });
            this.$cancelBtn.click(function () {
                that.hide();
                that.options.cancelCallBack();
            });
            this.$submitBtn.click(function () {
                $(this).attr("disabled","disabled");
                var data = that.formToJson();

                // 提交时在进行校验一次
                if(!that.verifyForm(that)){
                    $(this).removeAttr("disabled");
                    return ;
                }
                that.options.submitCallBack(data);

            });
            this.$popupClose.click(function () {
                that.hide();
                that.options.cancelCallBack();
            });
          
            that.selectList(that);

            if(!that.blurVerifyForm(that)){
                return;
            }

            this.resize();
            this.hide();

        };

        this.createForm = function (options) {
            var that = this, data = options['data'];
            this.$form = $("<form class='popupFrom'></form>");
            for (var i in data) {
                var row = data[i];
                for (var id in row) {
                    var field = row[id],
                        label = field['description'],
                        required = field['required'],
                        minLength = field['minLength'],
                        type = field['type'],
                        pattern = field['pattern'],
                        disabled = field['inputDisabled'],
                        defaultValue = field['default'],
                        message = field['message'],
                        port = field['isPort'],
                        weight = field['isWeight'],
                        note = field["note"],
                        remind = field["remind"];
                        module = field["module"];
                        if (id == "MASTER_ID")
                            label = "主节点";
                        if("select" == module){
                            label && $("<fieldset class='popupFormFiedSet'>" +
                                "<div class='popupFromGroup'>" +
                                "<label class='popupFormLabel' style="  + (remind ? 'color:red':'color:#333333') + ">" + label + "</label>" +
                                "<div class = 'popupFormDivSelect'>" +
                                "<select name='" + id + "'>" +
                                "</select>" +
                                "</div>" +
                                "</div>" +
                                "</fieldset>").appendTo(that.$form);
                        } else if("textarea" == module){
                            label && $("<fieldset class='popupFormFiedSet'>" +
                            "<div class='popupFromGroup'>" +
                            "<label class='popupFormLabel' style=" + (remind ? 'color:red' : 'color:#333333' )+ ">" + label + "</label>" +
                            "<div class = 'popupFormDivTextArea'>" +
                            "<textarea name='" + id + "'>" + defaultValue +
                            "</textarea>" +
                            "</div>" +
                            "</div>" +
                            "</fieldset>").appendTo(that.$form);
                        } else if (id != "OS_PWD") {
                            var dynamicInput = "";
                            if (typeof (pattern) != "undefined") {
                                dynamicInput = "<input class = 'popupFormInput'  pattern='" + pattern + "' name='" + id;
                            } else {
                                dynamicInput = "<input class = 'popupFormInput'  name='" + id;
                            }
                            if("passInput" == module){
                                var password = "password";
                                dynamicInput += "'type ='"+password;
                            }

                            if (defaultValue) {
                                dynamicInput += "'value ='"+defaultValue;
                            }
                            if (message) {
                                dynamicInput += "'message ='"+message;
                            }
                            if (port) {
                                dynamicInput += "'isPort ='"+port;
                            }
                            if (weight) {
                                dynamicInput += "'isWeight ='"+weight;
                            }
                            if (label) {
                                dynamicInput += "'description ='"+label;
                            }
                            if (disabled) {
                                dynamicInput += "' disabled=true >";
                            } else {
                                dynamicInput += "' />";
                            }
                            label && $("<fieldset class='popupFormFiedSet'>" +
                                "<div class='popupFromGroup'>" +
                                "<label class='popupFormLabel' style=" + (remind ? 'color:red' : 'color:#333333' )+ ">" + label + "</label>" +
                                "<div class = 'popupFormDivInput'>" +
                                dynamicInput +
                                // (disabled === true ? "<input class = 'popupFormInput' name='" + id + "' disabled=true >" :
                                // "<input class = 'popupFormInput' name='" + id + "' />") +
                                "</div>" +
                                "</div>" +
                                "</fieldset>").appendTo(that.$form);
                        } 
                }
            }
           
            //解决出现滚动条的时候 FF和IE 下出现padding-bottom丢失情况，把原先的padding-bottom设置为0，最后一个元素添加padding高度
            $("<div style='height:15px'></div>").appendTo(that.$form);
        };

        this.selectList = function (that){
            var data = that.options.data;
            for (var i in data) {
                var row = data[i];
                for (var id in row) {
                    if(id == "SSH_ID"){
                        //初始化服务器的IP和用户列表，绑定事件
                        var serverList = "";
                        var ipSelect = that.$form.find("select[name='SSH_ID']");
                        var arr = JSON.parse(that.options.userInfo);
                        for (var i in arr) {
                            var sshList = arr[i].SSH_LIST;
                            var ip = arr[i].SERVER_IP;
                            for (var j in sshList) {
                                var label = sshList[j].SSH_NAME + "@" + ip;
                                serverList += "<option value='" + sshList[j].SSH_ID + "'>" + label + "</option>";
                            }
                        }
                        ipSelect.html(serverList);
                    }else if("input" == row[id].module && id == "SERV_INST_ID"){
                        var instIdInput = that.$form.find("input[name='SERV_INST_ID']");
                        instIdInput.val(self.topoData.INST_ID);
                    }else if("select" == row[id].module && (id == "SERV_INST_ID" || id =="REDIS_CLUSTER_CACHE" || id =="REDIS_CLUSTER_QUEUE" 
                            || id == "CLICKHOUSE_SERV" || id == "ORACLE_DG_SERV" || id == "ROCKETMQ_SERV" || id == "REDIS_CLUSTER_PFM" || id == "REDIS_CLUSTER_IPNUM")){
                        var ServerList = "";
                        var Select = that.$form.find("select[name='"+ id +"']");
                        var arrOptions;
                        if(id == "REDIS_CLUSTER_CACHE" || id == "REDIS_CLUSTER_PFM" || id == "REDIS_CLUSTER_IPNUM" || id == "SERV_INST_ID"){
                            arrOptions = JSON.parse(that.options.cacheCluster);
                        }else if(id == "REDIS_CLUSTER_QUEUE"){
                            arrOptions = JSON.parse(that.options.cacheHaCluster);
                        }else if(id == "ORACLE_DG_SERV"){
                            arrOptions = JSON.parse(that.options.oraDbCluster);
                        }else if(id == "CLICKHOUSE_SERV"){
                            arrOptions = JSON.parse(that.options.clickHouseCluster);
                        }else if(id == "ROCKETMQ_SERV" ){
                            arrOptions = JSON.parse(that.options.mqCluster);
                        }else{
                            arrOptions = JSON.parse(that.options.userInfo);
                        }

                        for (var i in arrOptions) {
                            var label = arrOptions[i].SERV_NAME;
                            ServerList += "<option value='" + arrOptions[i].INST_ID + "'>" + label + "</option>";
                        }
                        Select.html(ServerList);
                    }else if(id == "NODE_TYPE"){
                        // redis主从节点
                        var msSelectList = " ";
                        var msSelect = that.$form.find("select[name='NODE_TYPE']");
                        msSelectList += "<option value='1'>主节点</option>";
                        msSelectList += "<option value='0'>从节点</option>";

                        msSelect.html(msSelectList);
                    }else if(id == 'VERSION'){
                        var VersionServerList = "";
                        var VersionSelect = that.$form.find("select[name='"+ id +"']");
                        VersionSelect.attr("autocomplete","off");
                        var arrList = that.options.servVersion;
                        var type = plate.PlateType;
                        var version = arrList[type].VERSION;
                        var versionList = version.split(",");
                        for(var i=0; i<versionList.length; i++){
                           if(plate.version==versionList[i]){
                                VersionServerList += "<option value='" + versionList[i] + "' selected>" + versionList[i] + "</option>"; 
                            }else{
                                VersionServerList += "<option value='" + versionList[i] + "'>" + versionList[i] + "</option>"; 
                            }
                        }
                        VersionSelect.html(VersionServerList);
                    }else{
                        var field = row[id];
                        var note = field['note'];
                        var SelectList = "";
                        var Select = that.$form.find("select[name='"+ id +"']");
                        if(note){
                            var option = note.split('|');
                            for(var j = 0; j<option.length; j++){
                                var kvPair = option[j].split(':');
                                if (kvPair.length > 1)
                                    SelectList += "<option value="+kvPair[1]+">"+kvPair[0]+"</option>";
                                else
                                    SelectList += "<option value="+option[j]+">"+option[j]+"</option>";
                            }
                        }   
                        Select.html(SelectList);
                       
                    }
                    
                }
            }
         
        };
        
        this.setservVersion = function (dataInfo) {  
            this.options.servVersion = dataInfo;
        };
        
        //IP、用户和密码相关的函数
        this.setUserInfo = function (dataInfo,type) {
            switch(type){
               case "DB_ORACLE_DG":
                    this.options.oraDbCluster = dataInfo;
                    break;
                case "DB_CLICKHOUSE":
                    this.options.clickHouseCluster = dataInfo;
                    break;
                case "CACHE_REDIS_CLUSTER":
                    this.options.cacheCluster = dataInfo;
                    break;
                case "CACHE_REDIS_HA_CLUSTER":
                    this.options.cacheHaCluster = dataInfo;
                    break;
                case "MQ_ROCKETMQ":
                    this.options.mqCluster = dataInfo;
                    break;
                default:
                    this.options.userInfo = dataInfo;
            }
        };
        
        this.getUsersByIP = function () {
            var ip = this.$form.find("select[name='SSH_ID']").val();
            var userList = "";
            for (var i in this.options.userInfo) {
                var server = this.options.userInfo[i];
                if (server.SERVER_IP === ip) {
                    for (var j in server.SSH_LIST) {
                        var user = server.SSH_LIST[j].SSH_NAME;
                        userList += "<option value='" + user + "'>" + user + "</option>";
                    }
                    break;
                }
            }
            this.$form.find("select[name='OS_USER']").html(userList);
        };
        
        this.getPwdByUser = function () {
            var ip = this.$form.find("select[name='IP']").val();
            var userName = this.$form.find("select[name='OS_USER']").val();
            for (var i in this.options.userInfo) {
                var server = this.options.userInfo[i];
                if (server.SERVER_IP === ip) {
                    for (var j in server.SSH_LIST) {
                        var user = server.SSH_LIST[j];
                        if (user.SSH_NAME === userName) {
                            return user.SSH_PWD;
                        }
                    }
                }
            }
        };

        // 弹窗提交表单校验
        this.verifyForm = function(that){ 
            var form = that.$form.find('input');
            var brokerRole = that.$form.find('select[name=BROKER_ROLE]').find("option:selected").text();
            for(var i=0; i<form.length; i++){    
                var value = $(form[i]).val();
                var message = $(form[i]).attr("message");
                var reg = new RegExp($(form[i]).attr('pattern'));
                var port = $(form[i]).attr('isPort');
                var weight = $(form[i]).attr('isWeight');
                var description = $(form[i]).attr('description');
                var name =  $(form[i]).attr('name');
                
                if($(form[i]).attr('pattern')){
                    if(port){
                        if(reg.test(value) || value == '' || value <= 1024 || value >= 65535){
                            Util.msg(message);
                            return false;
                        } 
                    }else if(weight){
                        if (reg.test(value) || value == '' || value < 0 || value > 100) {
                            Util.msg(message);
                            return false;
                        } 
                    }else{
                        if(reg.test(value) || value == ''){
                            Util.msg(message);
                            return false;
                        }
                    }   
               }else{
                    if(value == ''){
                        if(name == 'SPECIAL_REPORT_CUSTID'){
                            return true
                        }
                        Util.msg(description+"不能为空");
                        return false;
                    }
               }  
            }
            if (brokerRole == '请选择') {
                Util.msg("broker-role没有选择，请选择！");
                return false;
            }

            return true;

        };
        
        this.blurVerifyForm = function(that){
            var form = that.$form.find('input');
            for(var i=0; i<form.length; i++){  
                var name =  $(form[i]).attr('name');    
                $(form[i]).blur(function () {
                    var value = $(this).val();
                    var description = $(this).attr('description');
                    if (value == '') {
                        if(name == "SPECIAL_REPORT_CUSTID"){
                            return true
                        }
                        Util.msg(description+"不能为空！");
                        return false
                    }
                });      
            }
            return true
        };
    };
    $.extend({
        popupForm: function (eleType, schema, submitCallBack, cancelCallBack) {
            var eleTypeJson,
                fields = [],
                options = {
                    submitCallBack: submitCallBack,
                    cancelCallBack: cancelCallBack
                },
                res = {};
            
            analysis(eleType, schema['properties']);
            getFields(eleTypeJson);
            options.data = fields;
            
            function analysis(eleType, schema) {
                if (eleTypeJson) {
                    return;
                }
                if (schema[eleType]) {
                    eleTypeJson = schema[eleType];
                    return;
                }
                for (var key in schema) {
                    var jsonRoot;
                    if (schema[key]['type'] === 'array') {
                        jsonRoot = schema[key]['items']['properties'];
                    } else {
                        jsonRoot = schema[key]['properties'];
                    }
                    jsonRoot && analysis(eleType, jsonRoot);
                }
            }

            function getFields(json) {
                if (json == null || json == undefined)
                    return;
                
                var type = json['type'];
                if (type == null)
                    return;
                
                if (type === 'array') {
                    getFields(json['items']);
                } else if (type === 'object') {
                    var properties = json['properties'];
                    for (var key in properties) {
                        var field = new Object();
                        field[key] = properties[key];
                        /*var option = {key:properties[key]};*/
                        fields.push(field);
                    }
                }
            }

            res = new popupForm(eleType, options);
            return res;
        }
    });
})(jQuery);