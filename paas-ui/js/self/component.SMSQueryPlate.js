var Component = window.Component || {};


(function(Component) {
    
    /**
     * SmsQuery面板类
     */
    function SMSQueryPlate(url, id, name, canvas, isProduct) {
        this.loadSchema("sms.query.schema");
        
        Util.showLoading();
        this.showMetaDataOnMouse = false;
        this.setRootUrl(url);
        this.PlateType = "SMS_QUERY_SERVICE";
        
        //调用父类方法初始化舞台
        var data = null;
        this.initStage(id, name, canvas, isProduct);
        data = this.getTopoData(id);
        if (data == null) {
            Util.hideLoading();
            return;
        } else if (data == "init") {
            data = null;
        }

        //图标(暂定)
        this.SmsQueryIcon = "sms_query.png";
        this.NgxIcon = "nginx.png";
        
        //常量
        this.NGX_CONST = "NGX";
        this.NGX_CONTAINER_CONST = "NGX_CONTAINER";
        
        this.SMS_QUERY_CONST = "SMS_QUERY";
        this.SMS_QUERY_CONTAINER_CONST = "SMS_QUERY_CONTAINER";
        
        this.NgxContainer = null;
        this.SmsQueryContainer = null;
        
        var self = this;
        
        //初始化右键菜单
        this.nodeMenu = $.contextMenu({
            items:[
                {label:'部署组件', icon:'../images/console/icon_install.png', callback: function(e){
                    self.deployElement(e.target);
                }},
                {label:'修改信息', icon:'../images/console/icon_edit.png', callback: function(e){
                    self.popupForm(e.target,"edit");
                }},
                {label:'删除组件', icon:'../images/console/icon_delete.png', callback: function(e){
                    var element = e.target;
                    layer.confirm("确认删除组件吗？", {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.deleteComponentBackground(element);
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
        this.deployedMenu = $.contextMenu({
            items:[
                {label:'查看信息', icon:'../images/console/icon_delete.png', callback: function(e){
                    self.popupForm(e.target,"view");
                }},
                {label:'卸载(缩容)', icon:'../images/console/icon_delete.png', callback: function(e){
                    self.undeployElement(e.target);
                }},
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
                {label:'更新', icon:'../images/console/icon_update.png', callback: function(e){
                    layer.confirm('确认要更新“'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.updateElement(e.target);
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
        
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        this.NgxForm = $.popupForm(this.NGX_CONST, window['sms.query.schema'], function(json){
            self.saveElementData(self.popElement, json, self.NgxForm);
        }, cancelFunction);
        this.SmsQueryForm = $.popupForm(this.SMS_QUERY_CONST, window['sms.query.schema'], function(json){
            self.saveElementData(self.popElement, json, self.SmsQueryForm);
        }, cancelFunction);
        
        this.clusterType = ["MQ_ROCKETMQ","DB_ORACLE_DG","CACHE_REDIS_CLUSTER","CACHE_REDIS_HA_CLUSTER","DB_CLICKHOUSE","SMS"];
        for(var i=0; i<this.clusterType.length; i++){
            var type = this.clusterType[i];
            if(type == 'SMS'){
                this.getSSHList(type, [this.NgxForm, this.SmsQueryForm]);
                this.getServVersion(type, [this.NgxForm, this.SmsQueryForm]);
            }else{
                this.getServListByServType(type, [this.NgxForm, this.SmsQueryForm]);
            }
        }

        //初始化Container
        this.initContainer(data);
    }

    SMSQueryPlate.prototype = new Component.Plate();
    Component.SMSQueryPlate = SMSQueryPlate;

    /**
     * 初始化container及各个容器的下属实例
     */
    SMSQueryPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.SMS_QUERY_SERV_CONTAINER;
            this.needInitTopo = false;

            var NGX_CONTAINER = topoData.NGX_CONTAINER;
            var SMS_QUERY_CONTAINER = topoData.SMS_QUERY_CONTAINER;

            this.NgxContainer = this.makeContainer(
                NGX_CONTAINER.POS.x, NGX_CONTAINER.POS.y,
                'NGX容器',
                NGX_CONTAINER.POS.row, NGX_CONTAINER.POS.col, "container");
            this.NgxContainer._id = NGX_CONTAINER.INST_ID;

            this.SmsQueryContainer = this.makeContainer(
                SMS_QUERY_CONTAINER.POS.x, SMS_QUERY_CONTAINER.POS.y,
                'SMS_QUERY容器',
                SMS_QUERY_CONTAINER.POS.row, SMS_QUERY_CONTAINER.POS.col, "container");
            this.SmsQueryContainer._id = SMS_QUERY_CONTAINER.INST_ID;
            
            this.setMetaData(this.NgxContainer, NGX_CONTAINER);
            this.setMetaData(this.SmsQueryContainer, SMS_QUERY_CONTAINER);
            
            for (var i=0; i<NGX_CONTAINER.NGX.length; i++) {
                var ngx = NGX_CONTAINER.NGX[i];
                var node = this.addNodeToContainer(this.NgxContainer.x+1, this.NgxContainer.y+1, this.iconDir+this.NgxIcon,
                    ngx.INST_ID, this.NGX_CONST, this.nodeMenu,
                    this.NgxContainer, true, false,
                    STATUS_UNDEPLOYED);
                this.setMetaData(node, ngx);
            }
            
            for (var i=0; i<SMS_QUERY_CONTAINER.SMS_QUERY.length; i++) {
                var smsQuery = SMS_QUERY_CONTAINER.SMS_QUERY[i];
                var node = this.addNodeToContainer(this.SmsQueryContainer.x+1, this.SmsQueryContainer.y+1, this.iconDir+this.SmsQueryIcon,
                    smsQuery.INST_ID, this.SMS_QUERY_CONST, this.nodeMenu,
                    this.SmsQueryContainer, true, false,
                    STATUS_UNDEPLOYED);
                this.setMetaData(node, smsQuery);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.NgxContainer = this.makeContainer(this.width*0.5, this.height*0.3, "NGX容器", 1, 1, "container");
            this.SmsQueryContainer = this.makeContainer(this.width*0.5, this.height*0.7, "SMS_QUERY容器", 1, 1, "container");
        }
        
        //添加container连接
        link1 = new JTopo.Link(this.NgxContainer, this.SmsQueryContainer);
        link1.direction = 'vertical';
        this.scene.add(link1);

        Util.hideLoading();
    }

    // 保存面板拓扑信息(位置信息等)
    SMSQueryPlate.prototype.toPlateJson = function(_needCollectd) {
        var SMS_QUERY_SERV_CONTAINER = {};
        SMS_QUERY_SERV_CONTAINER.INST_ID = this.id;
        
        // NGX集群
        var NGX_CONTAINER = {};
        NGX_CONTAINER.INST_ID = this.NgxContainer._id;
        NGX_CONTAINER.POS = this.NgxContainer.getPosJson();
        var NGX = [];
        NGX_CONTAINER.NGX = NGX;
        SMS_QUERY_SERV_CONTAINER.NGX_CONTAINER = NGX_CONTAINER;

        // SMS_QUERY集群
        var SMS_QUERY_CONTAINER = {};
        SMS_QUERY_CONTAINER.INST_ID = this.SmsQueryContainer._id;
        SMS_QUERY_CONTAINER.POS = this.SmsQueryContainer.getPosJson();
        var SMS_QUERY = [];
        SMS_QUERY_CONTAINER.SMS_QUERY = SMS_QUERY;
        SMS_QUERY_SERV_CONTAINER.SMS_QUERY_CONTAINER = SMS_QUERY_CONTAINER;
        
        return {"SMS_QUERY_SERV_CONTAINER": SMS_QUERY_SERV_CONTAINER};
    }
        
    // yugabyte面板新增组件
    SMSQueryPlate.prototype.newComponent = function(x, y, datatype) {
        var container, img, text;
        switch(datatype) {
        case this.NGX_CONST:
            container = this.NgxContainer;
            img = this.iconDir+this.NgxIcon;
            text = "NGX";
            break;
        case this.SMS_QUERY_CONST:
            container = this.SmsQueryContainer;
            img = this.iconDir+this.SmsQueryIcon;
            text = "SMS_QUERY";
            break;
        }
        return this.addNodeToContainer(x, y, img, text, datatype, this.nodeMenu, container, false, true, STATUS_NEW) != null;
    }

    // 弹出窗口
    SMSQueryPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.NGX_CONST:
            this.NgxForm.show(this.getMetaData(element),type);
            break;
        case this.SMS_QUERY_CONST:
            this.SmsQueryForm.show(this.getMetaData(element),type);
            break;
        }
    }
        
    // 面板设置组件元数据
    SMSQueryPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        
        switch(element.type) {
        case this.NGX_CONST:
        case this.SMS_QUERY_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        }
        if(element.elementType == 'node'){
            element.text = data.VERSION;
        }
        element._id = id;
        // element.text = name;
        element.meta = data;
    }
        
    // 提取组件元数据
    SMSQueryPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.NGX_CONST:
        case this.SMS_QUERY_CONST:
            data.INST_ID = element._id;
            break;
        }
        for (var i in element.meta) {
            data[i] = element.meta[i];
        }
        return data;
    }

    // 组件部署成功时的处理
    SMSQueryPlate.prototype.getElementDeployed = function(element) {
        if (element.elementType == "node") {
            var self = this;
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
            if (element.parentContainer!=undefined && element.parentContainer != null) {
                element.removeEventListener('contextmenu');
                element.addEventListener('contextmenu', function(e) {
                    self.deployedMenu.show(e);
                });
            }
        }
    }

    // 卸载元素
    SMSQueryPlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.NGX_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "nginx的数量不能小于1！");
                res = false;
            }
            break;
        case this.SMS_QUERY_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "sms_query的数量不能小于1！");
                res = false;
            }
            break;
        }

        if (res) {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    }

    // 组件卸载成功时的处理
    SMSQueryPlate.prototype.getElementUndeployed = function(element) {
        if (element.elementType == "node") {
            element.status = "0";
            var self = this;
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.nodeMenu.show(e);
            });
        }
    }

})(Component);
