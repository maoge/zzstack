var Component = window.Component || {};


(function(Component) {
    
    /**
     * SMS_GATEWAY面板类
     */
    function SMSGwPlate(url, id, name, canvas, isProduct,ClassType,version) {
        this.loadSchema("sms.gateway.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        
        //调用父类方法初始化舞台
        var data = null;
        this.initStage(id, name, canvas, isProduct,ClassType,version);
        data = this.getTopoData(id);
        if (data == null) {
            Util.hideLoading();
            return;
        } else if (data == "init") {
            data = null;
        }
        this.PlateType = "SMS_GATEWAY";
        this.clusterType = ["MQ_ROCKETMQ","DB_ORACLE_DG","CACHE_REDIS_CLUSTER","CACHE_REDIS_HA_CLUSTER","SMS"];
        
        this.overridePlateMenu = true;
        this.showMetaDataOnMouse = true;

        this.getSmsABQueueWeight = "paas/metadata/getSmsABQueueWeightInfo";       //获取ABQueue权重

        //图标(暂定)
        this.SmsServerIcon = "sms-server.png";
        this.SmsProcessIcon = "sms-process.png";
        this.SmsClientIcon = "sms-client.png";
        this.SmsBatSaveIcon = "batchsave.png";
        this.SmsStatsIcon = "sms_stats.png";

        //常量
        this.SMS_SERVER_CONST = "SMS_SERVER";
        this.SMS_SERVER_EXT_CONST = "SMS_SERVER_EXT";
        this.SMS_PROCESS_CONST = "SMS_PROCESS";
        this.SMS_CLIENT_CONST = "SMS_CLIENT";
        this.SMS_BATSAVE_CONST = "SMS_BATSAVE";
        this.SMS_STATS_CONST = "SMS_STATS";
        
        this.SMS_SERVER_CONTAINER_CONST = "SMS_SERVER_CONTAINER";
        this.SMS_SERVER_EXT_CONTAINER_CONST = "SMS_SERVER_EXT_CONTAINER";
        this.SMS_PROCESS_CONTAINER_CONST = "SMS_PROCESS_CONTAINER";
        this.SMS_CLIENT_CONTAINER_CONST = "SMS_CLIENT_CONTAINER";
        this.SMS_BATSAVE_CONTAINER_CONST = "SMS_BATSAVE_CONTAINER";
        this.SMS_STATS_CONTAINER_CONST = "SMS_STATS_CONTAINER";

        this.SmsServerContainer = null;
        this.SmsServerExtContainer = null;
        this.SmsProcessContainer = null;
        this.SmsClientContainer = null;
        this.SmsBatSaveContainer = null;
        this.SmsStatsContainer = null;
        
        this.OracleDGPlate = null;

        // Util.hideLoading();
        var self = this;

        //初始化右键菜单
        this.nodeMenu = $.contextMenu({
            items:[
                {label:'部署组件', icon:'../images/console/icon_install.png', 
                    callback: function(e){ self.deployElement(e.target); }
                },
                {label:'修改信息', icon:'../images/console/icon_edit.png',
                    callback: function(e){ self.popupForm(e.target,"edit"); }
                },
                {label:'删除组件', icon:'../images/console/icon_delete.png',
                    callback: function(e){
                        var element = e.target;
                        layer.confirm("确认删除组件吗？", {
                            btn: ['是','否'], //按钮
                            title: "确认"
                        }, function(){
                            layer.close(layer.index);
                            self.deleteComponentBackground(element);
                        });
                    }
                },
                {label:'error日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"error"); }
                },
                {label:'info日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"info"); }
                },
                {label:'stdout日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"stdout"); }
                }
            ]
        });
        this.deployedMenu = $.contextMenu({
            items:[
                {label:'查看信息', icon:'../images/console/icon_delete.png',
                    callback: function(e){ self.popupForm(e.target,"view"); }
                },
                {label:'查看jvm监控', icon:'../images/console/icon_delete.png',
                    callback: function(e){ window.open(rootUrl + "console/index.html?instId=" + e.target._id); }
                },
                {label:'卸载(缩容)', icon:'../images/console/icon_delete.png',
                    callback: function(e){ self.undeployElement(e.target); }
                },
                {label:'启动', icon:'../images/console/icon_start.png', callback: function(e){
                    layer.confirm('确认要启动“'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.startElement(e.target);
                    });
                }},
                {label:'停止', icon:'../images/console/icon_stop.png', callback: function(e){
                    layer.confirm('确认要停止“'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.stopElement(e.target);
                    });
                }},
                {label:'单个更新', icon:'../images/console/icon_update.png', callback: function(e){
                    layer.confirm('确认要更新“'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.updateElement(e.target);
                    });
                }},
                {label:'批量更新', icon:'../images/console/icon_update.png', callback: function(){
                    layer.confirm('确认要更新选中的组件吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.batchUpdateElement();
                    });
                }},
                {label:'error日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"error"); }
                },
                {label:'info日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"info"); }
                },
                {label:'stdout日志', icon:'../images/console/icon_log.png',
                    callback: function(e){ self.showAppLog(e.target,"stdout"); }
                } 
            ]
        });
        
        this.plateMenu = $.contextMenu({
            items:[
                {label:'保存面板结构', icon:'../images/console/icon_save.png', callback: function(e){
                        self.saveTopoData();
                    }},
                {label:'部署面板', icon:'../images/console/icon_install.png', callback: function(e){
                        layer.confirm('确认要部署集群“'+self.name+'”吗？', {
                            btn: ['是','否'], //按钮
                            title: "确认"
                        }, function(){
                            layer.close(layer.index);
                            self.deployElement(e.target,1);
                        });
                    }},
                {label:'伪部署面板', icon:'../images/console/icon_pseudo_deployment.png', callback: function(e){
                        layer.confirm('确认要伪部署集群“'+self.name+'”吗？', {
                            btn: ['是','否'], //按钮
                            title: "确认"
                        }, function(){
                            layer.close(layer.index);
                            self.deployElement(e.target,2);
                        });
                    }},
                {label:'卸载面板', icon:'../images/console/icon_uninstall.png', callback: function(e){
                        layer.confirm('确认要卸载集群“'+self.name+'”吗？', {
                            btn: ['是','否'], //按钮
                            title: "确认"
                        }, function(){
                            layer.close(layer.index);
                            self.unDeployPlate(e.target);
                        });
                    }},
                {label:'强制卸载面板', icon:'../images/console/icon_uninstall.png', callback: function(e){
                        layer.confirm('确认要卸载集群“'+self.name+'”吗？', {
                            btn: ['是','否'], //按钮
                            title: "确认"
                        }, function(){
                            layer.close(layer.index);
                            self.forceUndeployPlate(e.target);
                        });
                    }},
                {label:'调整 A/B Quece权重', icon:'../images/console/icon_pseudo_deployment.png', callback: function(e){
                    self.smsId = self.id;
                    var isDeployed = false;
                    var data = self.getTopoData(self.id);
                    if(data == null || data == "init"){
                        Component.Alert("warn", "容器中组件不存在，请处理！");
                        return;
                    }
                    if(data.DEPLOY_FLAG.length>0){
                        for(var i=0; i<data.DEPLOY_FLAG.length; i++){
                            var isDeployedValue = data.DEPLOY_FLAG[i][self.id];
                            if(!Number(isDeployedValue)){
                                continue;
                            }else{
                                isDeployed = true;
                                break;
                            }
                        }
                    }
                    if(isDeployed != true){
                        Component.Alert("warn", "短信网关未部署！");
                        return;
                    }
                    var reqData = {};
                    smsGwId = self.id;
                    reqData.SERV_INST_ID = self.id;
                    $.ajax({
                        url: self.url + self.getSmsABQueueWeight,
                        type: "post",
                        dataType: "json",
                        contentType: "application/json; charset=utf-8",
                        data: JSON.stringify(reqData),
                        success: function (result) {
                            if (result.RET_CODE == 0) {
                                initAbQueueWeight = result.RET_INFO;
                                withdrawSmsABQueueWeight();
                                INST_ID = [];
                                INST_ID.push(initAbQueueWeight.RedisClusterA.SERV_INST_ID);
                                INST_ID.push(initAbQueueWeight.RedisClusterB.SERV_INST_ID);
                                mm = new JqMonitor({"INST_ID": INST_ID, "TIME_INTERVAL": $("#timeInterval").val()});
                                mmTimer = window.setInterval(function(){
                                    mm.init();
                                }, 10000);
                                $('#adjustWeight').modal("show");
                                $(".modal-backdrop").appendTo($("#mainContent"));
                                self.abQueueWeighPopupForm();
                            }else{
                                Component.Alert("warn", "A/B Queue集群未初始化！");
                            }
                        }
                    });
                    
                    
                    
                }},
                {label:'Oracle-DG 主从切换', icon:'../images/console/icon_pseudo_deployment.png', callback: function(e){
                    var id= self.getOracleDgId(e.scene.childs);
                    self.smsId = self.id;
                    var isDeployed = false;
                    var data = self.getTopoData(self.id);
                    if(data == null || data == "init"){
                        Component.Alert("warn", "容器中组件不存在，不可以进行主从切换，请处理！");
                        return;
                    }
                    if(data.DEPLOY_FLAG.length>0){
                        for(var i=0; i<data.DEPLOY_FLAG.length; i++){
                            var isDeployedValue = data.DEPLOY_FLAG[i][self.id];
                            if(!Number(isDeployedValue)){
                                continue;
                            }else{
                                isDeployed = true;
                                break;
                            }
                        }
                    }
                    if(isDeployed != true){
                        Component.Alert("warn", "短信网关未部署，请部署后在进行主从切换！");
                        return;
                    }
                    $('#changeMs').modal("show");
                    $(".modal-backdrop").appendTo($("#canvas1"));
                    
                    if (this.OracleDGPlate == null) {
                        this.OracleDGPlate = new Component.DBOracleDGPlate(rootUrl, id, name, $("#canvas1")[0], "1", false);
                    }
                }}
            ]
        });
        this.containerMenu = $.contextMenu({
            items:[
                {label:'修改版本', icon:'../images/console/icon_edit.png', callback: function(e){
                    if(e.target.text== 'SMS_SERVER容器'){
                        e.target.type = self.SMS_SERVER_CONTAINER_CONST;
                    }else if(e.target.text== 'SMS_SERVER_EXT容器'){
                        e.target.type = self.SMS_SERVER_EXT_CONTAINER_CONST;
                    }else if(e.target.text== 'SMS_PROCESS容器'){
                        e.target.type = self.SMS_PROCESS_CONTAINER_CONST;
                    }else if(e.target.text== 'SMS_CLIENT容器'){
                        e.target.type = self.SMS_CLIENT_CONTAINER_CONST;
                    }else if(e.target.text== 'SMS_BATSAVE容器'){
                        e.target.type = self.SMS_BATSAVE_CONTAINER_CONST;
                    }else if(e.target.text== 'SMS_STATS容器'){
                        e.target.type = self.SMS_STATS_CONTAINER_CONST;
                    }
            
                    self.popupForm(e.target,"edit");
                }}
            ]
        })
        
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };

        this.SmsServerForm = $.popupForm(this.SMS_SERVER_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsServerForm);
        }, cancelFunction);
        this.SmsServerExtForm = $.popupForm(this.SMS_SERVER_EXT_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsServerExtForm);
        }, cancelFunction);
        this.SmsProcessForm = $.popupForm(this.SMS_PROCESS_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsProcessForm);
        }, cancelFunction);
        this.SmsClientForm = $.popupForm(this.SMS_CLIENT_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsClientForm);
        }, cancelFunction);
        this.SmsBatSaveForm = $.popupForm(this.SMS_BATSAVE_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsBatSaveForm);
        }, cancelFunction);
        this.SmsStatsForm = $.popupForm(this.SMS_STATS_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsStatsForm);
        }, cancelFunction);

        this.SmsServerContainerForm = $.popupForm( this.SMS_SERVER_CONTAINER_CONST , window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsServerContainerForm);
        }, cancelFunction);
        this.SmsServerExtContainerForm = $.popupForm( this.SMS_SERVER_EXT_CONTAINER_CONST , window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsServerExtContainerForm);
        }, cancelFunction);
        this.SmsProcessContainerForm = $.popupForm( this.SMS_PROCESS_CONTAINER_CONST , window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsProcessContainerForm);
        }, cancelFunction);
        this.SmsClientContainerForm = $.popupForm(this.SMS_CLIENT_CONTAINER_CONST , window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsClientContainerForm);
        }, cancelFunction);
        this.SmsBatSaveContainerForm = $.popupForm(this.SMS_BATSAVE_CONTAINER_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsBatSaveContainerForm);
        }, cancelFunction);
        this.SmsStatsContainerForm = $.popupForm( this.SMS_STATS_CONTAINER_CONST, window['sms.gateway.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsStatsContainerForm);
        }, cancelFunction);

        for(var i=0; i<this.clusterType.length; i++){
            if(this.clusterType[i] == 'SMS'){
                this.getSSHList(this.clusterType[i], [this.SmsServerForm, this.SmsServerExtForm, this.SmsProcessForm, this.SmsClientForm, this.SmsBatSaveForm, this.SmsStatsForm]);
                this.getServVersion(this.clusterType[i], [this.SmsServerForm, this.SmsServerExtForm, this.SmsProcessForm, this.SmsClientForm, this.SmsBatSaveForm, this.SmsStatsForm,
                    this.SmsServerContainerForm,this.SmsServerExtContainerForm,this.SmsProcessContainerForm,this.SmsClientContainerForm,this.SmsBatSaveContainerForm,this.SmsStatsContainerForm]);
            }else{
                this.getServListByServType(this.clusterType[i], [this.SmsServerForm, this.SmsServerExtForm, this.SmsProcessForm, this.SmsClientForm, this.SmsBatSaveForm, this.SmsStatsForm]);
            }
        }

        //初始化Container
        this.initContainer(data);
    }
    
    SMSGwPlate.prototype = new Component.Plate();
    Component.SMSGwPlate = SMSGwPlate;
    
    /**
     * 初始化container
     */
    SMSGwPlate.prototype.initContainer = function(data) {
        var self = this;
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.SMS_GATEWAY_SERV_CONTAINER;
            this.needInitTopo = false;

            var SMS_SERVER_CONTAINER = topoData.SMS_SERVER_CONTAINER;
            var SMS_SERVER_EXT_CONTAINER = topoData.SMS_SERVER_EXT_CONTAINER;
            var SMS_PROCESS_CONTAINER = topoData.SMS_PROCESS_CONTAINER;
            var SMS_CLIENT_CONTAINER = topoData.SMS_CLIENT_CONTAINER;
            var SMS_BATSAVE_CONTAINER = topoData.SMS_BATSAVE_CONTAINER;
            var SMS_STATS_CONTAINER = topoData.SMS_STATS_CONTAINER;

            this.SmsServerContainer = this.makeContainer(
                SMS_SERVER_CONTAINER.POS.x, SMS_SERVER_CONTAINER.POS.y,
                'SMS_SERVER容器',
                SMS_SERVER_CONTAINER.POS.row, SMS_SERVER_CONTAINER.POS.col, "container");
            this.SmsServerContainer._id = SMS_SERVER_CONTAINER.INST_ID;
            
            this.SmsProcessContainer = this.makeContainer(
                SMS_PROCESS_CONTAINER.POS.x, SMS_PROCESS_CONTAINER.POS.y, 
                'SMS_PROCESS容器', 
                SMS_PROCESS_CONTAINER.POS.row, SMS_PROCESS_CONTAINER.POS.col, "container");
            this.SmsProcessContainer._id = SMS_PROCESS_CONTAINER.INST_ID;

            this.SmsClientContainer = this.makeContainer(
                SMS_CLIENT_CONTAINER.POS.x, SMS_CLIENT_CONTAINER.POS.y, 
                'SMS_CLIENT容器', 
                SMS_CLIENT_CONTAINER.POS.row, SMS_CLIENT_CONTAINER.POS.col, "container");
            this.SmsClientContainer._id = SMS_CLIENT_CONTAINER.INST_ID;

            this.SmsBatSaveContainer = this.makeContainer(
                SMS_BATSAVE_CONTAINER.POS.x, SMS_BATSAVE_CONTAINER.POS.y, 
                'SMS_BATSAVE容器', 
                SMS_BATSAVE_CONTAINER.POS.row, SMS_BATSAVE_CONTAINER.POS.col, "container");
            this.SmsBatSaveContainer._id = SMS_BATSAVE_CONTAINER.INST_ID;

            this.SmsStatsContainer = this.makeContainer(
                SMS_STATS_CONTAINER.POS.x, SMS_STATS_CONTAINER.POS.y,
                'SMS_STATS容器',
                SMS_STATS_CONTAINER.POS.row, SMS_STATS_CONTAINER.POS.col, "container");
            this.SmsStatsContainer._id = SMS_STATS_CONTAINER.INST_ID;

            this.setMetaData(this.SmsServerContainer, SMS_SERVER_CONTAINER);
            this.setMetaData(this.SmsProcessContainer, SMS_PROCESS_CONTAINER);
            this.setMetaData(this.SmsClientContainer, SMS_CLIENT_CONTAINER);
            this.setMetaData(this.SmsBatSaveContainer, SMS_BATSAVE_CONTAINER);
            this.setMetaData(this.SmsStatsContainer, SMS_STATS_CONTAINER);

            for (var i=0; i<SMS_SERVER_CONTAINER.SMS_SERVER.length; i++) {
                var smsServer = SMS_SERVER_CONTAINER.SMS_SERVER[i];
                var node = this.addNodeToContainer(this.SmsServerContainer.x+1, 
                    this.SmsServerContainer.y+1, this.iconDir+this.SmsServerIcon, 
                    smsServer.INST_ID, this.SMS_SERVER_CONST, this.nodeMenu, 
                    this.SmsServerContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, smsServer);
            }
            
            for (var i=0; i<SMS_PROCESS_CONTAINER.SMS_PROCESS.length; i++) {
                var smsProcess = SMS_PROCESS_CONTAINER.SMS_PROCESS[i];
                var node = this.addNodeToContainer(this.SmsProcessContainer.x+1,
                    this.SmsProcessContainer.y+1, this.iconDir+this.SmsProcessIcon,
                    smsProcess.INST_ID, this.SMS_PROCESS_CONST, this.nodeMenu,
                    this.SmsProcessContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, smsProcess);
            }

            for (var i=0; i<SMS_CLIENT_CONTAINER.SMS_CLIENT.length; i++) {
                var smsClient = SMS_CLIENT_CONTAINER.SMS_CLIENT[i];
                var node = this.addNodeToContainer(this.SmsClientContainer.x+1,
                    this.SmsClientContainer.y+1, this.iconDir+this.SmsClientIcon,
                    smsClient.INST_ID, this.SMS_CLIENT_CONST, this.nodeMenu,
                    this.SmsClientContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, smsClient);
            }

            for (var i=0; i<SMS_BATSAVE_CONTAINER.SMS_BATSAVE.length; i++) {
                var smsBatSave = SMS_BATSAVE_CONTAINER.SMS_BATSAVE[i];
                var node = this.addNodeToContainer(this.SmsBatSaveContainer.x+1,
                    this.SmsBatSaveContainer.y+1, this.iconDir+this.SmsBatSaveIcon,
                    smsBatSave.INST_ID, this.SMS_BATSAVE_CONST, this.nodeMenu,
                    this.SmsBatSaveContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, smsBatSave);
            }

            for (var i=0; i<SMS_STATS_CONTAINER.SMS_STATS.length; i++) {
                var smsStats = SMS_STATS_CONTAINER.SMS_STATS[i];
                var node = this.addNodeToContainer(this.SmsStatsContainer.x+1,
                    this.SmsStatsContainer.y+1, this.iconDir+this.SmsStatsIcon,
                    smsStats.INST_ID, this.SMS_STATS_CONST, this.nodeMenu,
                    this.SmsStatsContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, smsStats);
            }
            
            if (SMS_SERVER_EXT_CONTAINER != null && SMS_SERVER_EXT_CONTAINER.SMS_SERVER_EXT != null) {
                this.SmsServerExtContainer = this.makeContainer(
                    SMS_SERVER_EXT_CONTAINER.POS.x, SMS_SERVER_EXT_CONTAINER.POS.y, 'SMS_SERVER_EXT容器',
                    SMS_SERVER_EXT_CONTAINER.POS.row, SMS_SERVER_EXT_CONTAINER.POS.col, "container");
                this.SmsServerExtContainer._id = SMS_SERVER_EXT_CONTAINER.INST_ID;
                
                this.setMetaData(this.SmsServerExtContainer, SMS_SERVER_EXT_CONTAINER);
                
                for (var i=0; i<SMS_SERVER_EXT_CONTAINER.SMS_SERVER_EXT.length; i++) {
                    var smsServerExt = SMS_SERVER_EXT_CONTAINER.SMS_SERVER_EXT[i];
                    var node = this.addNodeToContainer(this.SmsServerExtContainer.x+1, 
                        this.SmsServerExtContainer.y+1, this.iconDir+this.SmsServerIcon, 
                        smsServerExt.INST_ID, this.SMS_SERVER_EXT_CONST, this.nodeMenu, 
                        this.SmsServerExtContainer, true, false, STATUS_UNDEPLOYED);
                    this.setMetaData(node, smsServerExt);
                }
            } else {
                this.SmsServerExtContainer = this.makeContainer(this.width*0.2, this.height*0.5, "SMS_SERVER_EXT容器", 1, 1, "container");
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.SmsServerContainer = this.makeContainer(this.width*0.2, this.height*0.3, "SMS_SERVER容器", 4, 1, "container");
            this.SmsServerExtContainer = this.makeContainer(this.width*0.2, this.height*0.5, "SMS_SERVER_EXT容器", 1, 1, "container");
            this.SmsProcessContainer = this.makeContainer(this.width*0.5, this.height*0.3, "SMS_PROCESS容器", 4, 1, "container");
            this.SmsClientContainer = this.makeContainer(this.width*0.8, this.height*0.3, "SMS_CLIENT容器", 4, 1, "container");
            this.SmsBatSaveContainer = this.makeContainer(this.width*0.3, this.height*0.7, "SMS_BATSAVE容器", 4, 1, "container");
            this.SmsStatsContainer = this.makeContainer(this.width*0.7, this.height*0.7, "SMS_STATS容器", 4, 1, "container");
        }
        this.SmsServerContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });
        this.SmsServerExtContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });
        this.SmsProcessContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });
        this.SmsClientContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });
        this.SmsBatSaveContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });
        this.SmsStatsContainer.addEventListener('contextmenu',function(e){
            var newData = self.getTopoData(self.id);
            if(newData && newData != "init"){
                self.containerMenu.show(e);
            }
        });

        //添加container连接
        link1 = new JTopo.FlexionalLink(this.SmsServerContainer, this.SmsProcessContainer, "");
        link1.direction = 'horizontal';
        this.scene.add(link1);

        // FoldLink
        link2 = new JTopo.FlexionalLink(this.SmsServerExtContainer, this.SmsProcessContainer, "");
        link2.direction = 'horizontal';
        this.scene.add(link2);

        link3 = new JTopo.Link(this.SmsProcessContainer, this.SmsClientContainer);
        link3.direction = 'horizontal';
        this.scene.add(link3);

        Util.hideLoading();
    }
    
    /**
     * 面板拓扑信息(位置信息等)
     */
    SMSGwPlate.prototype.toPlateJson = function(needCollectd) {
        var SMS_GATEWAY_SERV_CONTAINER = {};
        SMS_GATEWAY_SERV_CONTAINER.INST_ID = this.id;
        
        // SMS_SERVER集群
        var SMS_SERVER_CONTAINER = {};
        SMS_SERVER_CONTAINER.INST_ID = this.SmsServerContainer._id;
        SMS_SERVER_CONTAINER.POS = this.SmsServerContainer.getPosJson();
        var SMS_SERVER = [];
        SMS_SERVER_CONTAINER.SMS_SERVER = SMS_SERVER;
        SMS_GATEWAY_SERV_CONTAINER.SMS_SERVER_CONTAINER = SMS_SERVER_CONTAINER;

        // SMS_SERVER_EXT集群
        var SMS_SERVER_EXT_CONTAINER = {};
        SMS_SERVER_EXT_CONTAINER.INST_ID = this.SmsServerExtContainer._id;
        SMS_SERVER_EXT_CONTAINER.POS = this.SmsServerExtContainer.getPosJson();
        var SMS_SERVER_EXT = [];
        SMS_SERVER_EXT_CONTAINER.SMS_SERVER_EXT = SMS_SERVER_EXT;
        SMS_GATEWAY_SERV_CONTAINER.SMS_SERVER_EXT_CONTAINER = SMS_SERVER_EXT_CONTAINER;

        // SMS_PROCESS集群
        var SMS_PROCESS_CONTAINER = {};
        SMS_PROCESS_CONTAINER.INST_ID = this.SmsProcessContainer._id;
        SMS_PROCESS_CONTAINER.POS = this.SmsProcessContainer.getPosJson();
        var SMS_PROCESS = [];
        SMS_PROCESS_CONTAINER.SMS_PROCESS = SMS_PROCESS;
        SMS_GATEWAY_SERV_CONTAINER.SMS_PROCESS_CONTAINER = SMS_PROCESS_CONTAINER;

        // SMS_CLIENT集群
        var SMS_CLIENT_CONTAINER = {};
        SMS_CLIENT_CONTAINER.INST_ID = this.SmsClientContainer._id;
        SMS_CLIENT_CONTAINER.POS = this.SmsClientContainer.getPosJson();
        var SMS_CLIENT = [];
        SMS_CLIENT_CONTAINER.SMS_CLIENT = SMS_CLIENT;
        SMS_GATEWAY_SERV_CONTAINER.SMS_CLIENT_CONTAINER = SMS_CLIENT_CONTAINER;

        // SMS_BATSAVE集群
        var SMS_BATSAVE_CONTAINER = {};
        SMS_BATSAVE_CONTAINER.INST_ID = this.SmsBatSaveContainer._id;
        SMS_BATSAVE_CONTAINER.POS = this.SmsBatSaveContainer.getPosJson();
        var SMS_BATSAVE = [];
        SMS_BATSAVE_CONTAINER.SMS_BATSAVE = SMS_BATSAVE;
        SMS_GATEWAY_SERV_CONTAINER.SMS_BATSAVE_CONTAINER = SMS_BATSAVE_CONTAINER;

        // SMS_STATS集群
        var SMS_STATS_CONTAINER = {};
        SMS_STATS_CONTAINER.INST_ID = this.SmsStatsContainer._id;
        SMS_STATS_CONTAINER.POS = this.SmsStatsContainer.getPosJson();
        var SMS_STATS = [];
        SMS_STATS_CONTAINER.SMS_STATS = SMS_STATS;
        SMS_GATEWAY_SERV_CONTAINER.SMS_STATS_CONTAINER = SMS_STATS_CONTAINER;
        
        return {"SMS_GATEWAY_SERV_CONTAINER": SMS_GATEWAY_SERV_CONTAINER};
    };
    
    /**
     * 面板新增组件
     */
    SMSGwPlate.prototype.newComponent = function(x, y, datatype) {
        var container, img, text;
        switch(datatype) {
            case this.SMS_SERVER_CONST:
                container = this.SmsServerContainer;
                img = this.iconDir+this.SmsServerIcon;
                text = "SMS_SERVER";
                break;
            case this.SMS_SERVER_EXT_CONST:
                container = this.SmsServerExtContainer;
                img = this.iconDir+this.SmsServerIcon;
                text = "SMS_SERVER_EXT";
                break;
            case this.SMS_PROCESS_CONST:
                container = this.SmsProcessContainer;
                img = this.iconDir+this.SmsProcessIcon;
                text = "SMS_PROCESS";
                break;
            case this.SMS_CLIENT_CONST:
                container = this.SmsClientContainer;
                img = this.iconDir+this.SmsClientIcon;
                text = "SMS_CLIENT";
                break;
            case this.SMS_BATSAVE_CONST:
                container = this.SmsBatSaveContainer;
                img = this.iconDir+this.SmsBatSaveIcon;
                text = "SMS_BATSAVE";
                break;
            case this.SMS_STATS_CONST:
                container = this.SmsStatsContainer;
                img = this.iconDir+this.SmsStatsIcon;
                text = "SMS_STATS";
                break;
            }
        return this.addNodeToContainer(x, y, img, text, datatype, this.nodeMenu, container, false, true, STATUS_NEW) != null;
    };
    
    /**
     * 弹出窗口
     */
    SMSGwPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.SMS_SERVER_CONST:
            this.SmsServerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_SERVER_EXT_CONST:
            this.SmsServerExtForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_PROCESS_CONST:
            this.SmsProcessForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_CLIENT_CONST:
            this.SmsClientForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_BATSAVE_CONST:
            this.SmsBatSaveForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_STATS_CONST:
            this.SmsStatsForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_SERVER_CONTAINER_CONST:
            this.SmsServerContainerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_SERVER_EXT_CONTAINER_CONST:
            this.SmsServerExtContainerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_PROCESS_CONTAINER_CONST:
            this.SmsProcessContainerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_CLIENT_CONTAINER_CONST:
            this.SmsClientContainerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_BATSAVE_CONTAINER_CONST:
            this.SmsBatSaveContainerForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_STATS_CONTAINER_CONST:
            this.SmsStatsContainerForm.show(this.getMetaData(element),type);
            break;
        }
        
    };

    SMSGwPlate.prototype.abQueueWeighPopupForm = function(){
        if(null != this.SmsServerContainer.childs){
            abQueveId = this.SmsServerContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }else if(null != this.SmsServerExtContainer.childs){
            abQueveId = this.SmsServerExtContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }else if(null != this.SmsProcessContainer.childs){
            abQueveId = this.SmsProcessContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }else if(null != this.SmsClientContainer.childs){
            abQueveId = this.SmsClientContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }else if(null != this.SmsBatSaveContainer.childs){
            abQueveId = this.SmsBatSaveContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }else if(null != this.SmsStatsContainer.childs){
            abQueveId = this.SmsStatsContainer.childs[0].meta.REDIS_CLUSTER_QUEUE;
        }
    };

    /**
     * 面板设置组件元数据
     */
    SMSGwPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
            case this.SMS_SERVER_CONST:
            case this.SMS_SERVER_EXT_CONST:
            case this.SMS_PROCESS_CONST:
            case this.SMS_CLIENT_CONST:
            case this.SMS_BATSAVE_CONST:
            case this.SMS_STATS_CONST:
                delete data.INST_ID;
                element.addEventListener('mouseover', function(e) {
                    that.showMetadata(e.target, e);
                });
                element.addEventListener('mouseout', function(e) {
                    that.hideMetadata(e.target);
                });
                break;

            case this.SMS_SERVER_CONTAINER_CONST:
            case this.SMS_SERVER_EXT_CONTAINER_CONST:
            case this.SMS_PROCESS_CONTAINER_CONST:
            case this.SMS_CLIENT_CONTAINER_CONST:
            case this.SMS_BATSAVE_CONTAINER_CONST:
            case this.SMS_STATS_CONTAINER_CONST:
                delete data.INST_ID;
                break;
        }
        if(element.elementType == 'node'){
            element.text = data.VERSION;
        }
        element._id = id;
        element.meta = data; //metadata
    }
    
    /**
     * 提取组件元数据
     */
    SMSGwPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.SMS_SERVER_CONST:
        case this.SMS_SERVER_EXT_CONST:
        case this.SMS_PROCESS_CONST:
        case this.SMS_CLIENT_CONST:
        case this.SMS_BATSAVE_CONST:
        case this.SMS_STATS_CONST:
        case this.SMS_SERVER_CONTAINER_CONST:
        case this.SMS_SERVER_EXT_CONTAINER_CONST:
        case this.SMS_PROCESS_CONTAINER_CONST:
        case this.SMS_CLIENT_CONTAINER_CONST:
        case this.SMS_BATSAVE_CONTAINER_CONST:
        case this.SMS_STATS_CONTAINER_CONST:
            data.INST_ID = element._id;
            break;
        }
        for (var i in element.meta) {
            data[i] = element.meta[i];
        }
        return data;
    }
    
    /**
     * 组件部署成功时的处理
     */
    SMSGwPlate.prototype.getElementDeployed = function(element) {
        var self = this;
        if (element.elementType == "node") {
            var embadded = element.meta.PRE_EMBEDDED;
            if (embadded != undefined && embadded == "true") {
                element.status = "5";
            } else {
                element.status = "1";
            }
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.deployedMenu.show(e);
            });
        } else if (element.elementType == "container") {
            if (element.parentContainer != undefined && element.parentContainer != null) {
                element.removeEventListener('contextmenu');
                element.addEventListener('contextmenu', function(e) {
                    self.deployedMenu.show(e);
                });
            }
        }
    };
    
    /**
     * 面板卸载
     */
    SMSGwPlate.prototype.undeployElement = function(element){
        Component.Plate.prototype.undeployElement.call(this, element);
    };
    
    /**
     * 组件卸载成功时的处理
     */
    SMSGwPlate.prototype.getElementUndeployed = function(element) {
        if (element.elementType == "node") {
            element.status = "0";
            var self = this;
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.nodeMenu.show(e);
            });
        }
    };

    // 获取oracledg最大的容器id
    SMSGwPlate.prototype.getOracleDgId = function(data){
        for(var i=0; i<data.length; i++){
            if(data[i].elementType =="container" ){
                if( data[i].childs.length>0){
                    return data[i].childs[0].meta.ORACLE_DG_SERV;
                }
            }
        }
    }

})(Component);
