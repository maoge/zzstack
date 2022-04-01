var Component = window.Component || {};


(function(Component) {
    /**
     * RocketMQ面板
     */
    function MQRocketMQPlate(url, id, name, canvas, isProduct) {
        this.loadSchema("mq.rocketmq.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "MQ_ROCKETMQ";
        this.overridePlateMenu = true;
        
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
        this.vbrokerIcon = "rocketmq-vbroker-node.png";
        this.brokerIcon = "rocketmq-broker-node.png";
        this.namesrvIcon = "rocketmq-namesrv-node.png";
        this.collectdIcon = "collectd_icon.png";
        this.consoleIcon = "rocketmq-console.png";
        
        //常量
        this.VBROKER_CONST = "ROCKETMQ_VBROKER";
        this.BROKER_CONST = "ROCKETMQ_BROKER";
        this.NAMESRV_CONST = "ROCKETMQ_NAMESRV";
        this.CONSOLE_CONST = "ROCKETMQ_CONSOLE";
        
        this.showMetaDataOnMouse = false;
        
        this.VbrokerContainer = null;
        this.NamesrvContainer = null;
        
        Util.hideLoading();
        var self = this;
        //初始化右键菜单
        this.plateMenu = $.contextMenu({
            items:[
                {label:'保存面板结构', icon:'../images/console/icon_save.png', callback: function(_e){
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
                    layer.confirm('确认要伪部署集群'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.deployElement(e.target, "2");
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
                }}
            ]
        });
        this.elementMenu =$.contextMenu({
            items:[
                {label:'查看信息', icon:'../images/console/icon_delete.png', callback: function(e){
                        self.popupForm(e.target,"view");
                    }},
                {label:'删除组件', icon:'../images/console/icon_delete.png', callback: function(e){
                    var element = e.target;
                    layer.confirm("确认删除容器吗？", {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);   
                        var data = self.VbrokerContainer.childs;
                        if(data.length>1){
                            for(var i=0; i<data.length; i++){
                                if(data[i]._id == element._id){
                                    if(data[i].childs.length>0){
                                        Component.Alert("warn","内部存在组件，无法删除；请将组件全部删除，在进行此操作！");
                                        return
                                    }
                                }       
                            }
                        }else if(data.length==1){
                            Component.Alert("warn","vbroker容器中至少存在一个组件！");
                            return
                        }
                        self.deleteComponentBackground(element);

                    });
                }}
            ]
        });
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
                   }}]
        });
        this.deployedMenu = $.contextMenu({
            items:[
                    {label:'查看信息', icon:'../images/console/icon_delete.png', callback: function(e){
                        self.popupForm(e.target,"view");
                    }},
                    {label:'卸载(缩容)', icon:'../images/console/icon_delete.png', callback: function(e){
                        self.undeployElement(e.target);
                    }}]
        });
        
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        
        this.VbrokerForm = $.popupForm(this.VBROKER_CONST, window['mq.rocketmq.schema'], function(json){
            self.saveElementData(self.popElement, json, self.VbrokerForm);
        }, cancelFunction);
        this.BrokerForm = $.popupForm(this.BROKER_CONST, window['mq.rocketmq.schema'], function(json){
            self.saveElementData(self.popElement, json, self.BrokerForm);
        }, cancelFunction);
        this.NamesrvForm = $.popupForm(this.NAMESRV_CONST, window['mq.rocketmq.schema'], function(json){
            self.saveElementData(self.popElement, json, self.NamesrvForm);
        }, cancelFunction);
        this.CollectdForm = $.popupForm(this.COLLECTD_CONST, window['mq.rocketmq.schema'], function(json){
            self.saveElementData(self.popElement, json, self.CollectdForm);
        }, cancelFunction);
        this.ConsoleForm = $.popupForm(this.CONSOLE_CONST, window['mq.rocketmq.schema'], function(json){
            self.saveElementData(self.popElement, json, self.ConsoleForm);
        }, cancelFunction);
        
        this.getSSHList("MQ", [this.VbrokerForm, this.BrokerForm, this.NamesrvForm, this.CollectdForm, this.ConsoleForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    MQRocketMQPlate.prototype = new Component.Plate();
    Component.MQRocketMQPlate = MQRocketMQPlate;
    
    /**
     * 初始化container
     */
    MQRocketMQPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.ROCKETMQ_SERV_CONTAINER;
            this.needInitTopo = false;

            var ROCKETMQ_VBROKER_CONTAINER = topoData.ROCKETMQ_VBROKER_CONTAINER;
            var ROCKETMQ_NAMESRV_CONTAINER = topoData.ROCKETMQ_NAMESRV_CONTAINER;      
            var vbrokers = ROCKETMQ_VBROKER_CONTAINER.ROCKETMQ_VBROKER  //【】
            var collectd = topoData.COLLECTD;
            var rmqConsole = topoData.ROCKETMQ_CONSOLE;
            
            this.VbrokerContainer = this.makeContainer(
                ROCKETMQ_VBROKER_CONTAINER.POS.x, ROCKETMQ_VBROKER_CONTAINER.POS.y,
                'vbroker容器',
                ROCKETMQ_VBROKER_CONTAINER.POS.row, ROCKETMQ_VBROKER_CONTAINER.POS.col, "container");
            this.VbrokerContainer._id = ROCKETMQ_VBROKER_CONTAINER.INST_ID;
            
            this.NamesrvContainer = this.makeContainer(
                ROCKETMQ_NAMESRV_CONTAINER.POS.x, ROCKETMQ_NAMESRV_CONTAINER.POS.y,
                'namesrv容器', 
                ROCKETMQ_NAMESRV_CONTAINER.POS.row, ROCKETMQ_NAMESRV_CONTAINER.POS.col, "container");
            this.NamesrvContainer._id = ROCKETMQ_NAMESRV_CONTAINER.INST_ID;
            for(var vbrokerIndex in vbrokers){
                var vbroker = vbrokers[vbrokerIndex];
                var container = this.addContainerToContainer( ROCKETMQ_VBROKER_CONTAINER.POS.x, ROCKETMQ_VBROKER_CONTAINER.POS.y,
                    'broker容器', this.VBROKER_CONST, 1, 2, this.elementMenu, this.VbrokerContainer, true);
                
                var brokers = vbroker.ROCKETMQ_BROKER;
                for(var brokerIndex in brokers){
                    var broker = brokers[brokerIndex];
					var node = this.addNodeToContainer(container.x +1, container.y +1,
						this.iconDir+this.brokerIcon, "broker", this.BROKER_CONST, this.nodeMenu, container, true, false, STATUS_UNDEPLOYED);
					this.setMetaData(node, broker);
                }
                this.setMetaData(container, vbroker);
            }

            for (var i=0; i<ROCKETMQ_NAMESRV_CONTAINER.ROCKETMQ_NAMESRV.length; i++) {
                var namesrv = ROCKETMQ_NAMESRV_CONTAINER.ROCKETMQ_NAMESRV[i];
                var node = this.addNodeToContainer(this.NamesrvContainer.x+1,
                    this.NamesrvContainer.y+1, this.iconDir+this.namesrvIcon,
                    namesrv.INST_ID, this.NAMESRV_CONST, this.nodeMenu,
                    this.NamesrvContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, namesrv);
            }

            if (collectd && !$.isEmptyObject(collectd)) {
                var x = collectd.POS ? collectd.POS.x : 0;
                var y = collectd.POS ? collectd.POS.y : 0;
                this.addCollectd(x, y, this.iconDir+this.collectdIcon, 
                        collectd.INST_ID, this.COLLECTD_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED),
                this.setMetaData(this.collectd, collectd);
            }
            
            if (rmqConsole && !$.isEmptyObject(rmqConsole)) {
                var x = rmqConsole.POS ? rmqConsole.POS.x : 0;
                var y = rmqConsole.POS ? rmqConsole.POS.y : 0;
                this.addRocketMQConsole(x, y, this.iconDir+this.consoleIcon, 
                        rmqConsole.INST_ID, this.CONSOLE_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED);
                this.setMetaData(this.RocketMQConsole, rmqConsole);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.VbrokerContainer = this.makeContainer(this.width*0.5, this.height*0.2, "vbroker容器", 1, 2, "container");
            this.NamesrvContainer = this.makeContainer(this.width*0.5, this.height*0.6, "namesrv容器", 1, 2, "container");
        }

        //添加container连接
        link = new JTopo.FlexionalLink(this.VbrokerContainer, this.NamesrvContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    MQRocketMQPlate.prototype.toPlateJson = function(needCollectd) {
        var ROCKETMQ_SERV_CONTAINER = {};
        ROCKETMQ_SERV_CONTAINER.INST_ID = this.id;
        
        //vbroker
        var ROCKETMQ_VBROKER_CONTAINER = {};
        ROCKETMQ_VBROKER_CONTAINER.INST_ID = this.VbrokerContainer._id;
        ROCKETMQ_VBROKER_CONTAINER.POS = this.VbrokerContainer.getPosJson();
        var ROCKETMQ_VBROKER = [];
        ROCKETMQ_VBROKER_CONTAINER.ROCKETMQ_VBROKER = ROCKETMQ_VBROKER;
        ROCKETMQ_SERV_CONTAINER.ROCKETMQ_VBROKER_CONTAINER = ROCKETMQ_VBROKER_CONTAINER;
        //namesrv
        var ROCKETMQ_NAMESRV_CONTAINER = {};
        ROCKETMQ_NAMESRV_CONTAINER.INST_ID = this.NamesrvContainer._id;
        ROCKETMQ_NAMESRV_CONTAINER.POS = this.NamesrvContainer.getPosJson();
        var ROCKETMQ_NAMESRV = [];
        ROCKETMQ_NAMESRV_CONTAINER.ROCKETMQ_NAMESRV = ROCKETMQ_NAMESRV;
        ROCKETMQ_SERV_CONTAINER.ROCKETMQ_NAMESRV_CONTAINER = ROCKETMQ_NAMESRV_CONTAINER;
        
        //当第一次保存面板或collectd为空时，不需要传collectd信息
        var collectd = {};
        if (needCollectd && this.collectd != null) {
            collectd.INST_ID = this.collectd._id;
            var pos = {};
            pos.x = this.collectd.x+this.collectd.width/2;
            pos.y = this.collectd.y+this.collectd.height/2;
            collectd.POS = pos;
            
            ROCKETMQ_SERV_CONTAINER.COLLECTD = collectd;
        }
        
        var rmqConsole = {};
        if (this.RocketMQConsole != null) {
            rmqConsole.INST_ID = this.RocketMQConsole._id;
            var pos = {};
            pos.x = this.RocketMQConsole.x+this.RocketMQConsole.width/2;
            pos.y = this.RocketMQConsole.y+this.RocketMQConsole.height/2;
            rmqConsole.POS = pos;
            
            ROCKETMQ_SERV_CONTAINER.ROCKETMQ_CONSOLE = rmqConsole;
        }
        
        return {"ROCKETMQ_SERV_CONTAINER": ROCKETMQ_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    MQRocketMQPlate.prototype.newComponent = function(x, y, datatype) {
        // var container, img, text;
        switch(datatype) {
        case this.VBROKER_CONST:
            return this.addContainerToContainer(x, y, 'vbroker', this.VBROKER_CONST, 1, 2,this.elementMenu, this.VbrokerContainer, false)!= null;
        case this.BROKER_CONST:
            var childs = this.VbrokerContainer.childs;
            var success;
            for (var i = 0; i<childs.length; i++) {
				var vbroker = childs[i];
                if (!vbroker.isInContainer(x, y)) {
                    continue;
                }
                if(this.addNodeToContainer(x, y, this.iconDir+this.brokerIcon, "broker", datatype, this.nodeMenu, vbroker, false, false, STATUS_NEW) != null){
                    success = true;
                    break;
                }
			}
            return success;
        case this.NAMESRV_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.namesrvIcon, 
                    "namesrv", datatype, this.nodeMenu, this.NamesrvContainer, false, true, STATUS_NEW) != null;
        case this.COLLECTD_CONST:
            return this.addCollectd(x, y, this.iconDir+this.collectdIcon, "collectd", datatype, this.nodeMenu, false, STATUS_NEW);
        case this.CONSOLE_CONST:
            return this.addRocketMQConsole(x, y, this.iconDir+this.consoleIcon, "rocketmq-console", datatype, this.nodeMenu, false, STATUS_NEW);
        }
    };
    
    /**
     * 弹出窗口
     */
    MQRocketMQPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.VBROKER_CONST:
            this.VbrokerForm.show(this.getMetaData(element),type);
            break;
        case this.BROKER_CONST:
            this.BrokerForm.show(this.getMetaData(element),type);
            break;
        case this.NAMESRV_CONST:
            this.NamesrvForm.show(this.getMetaData(element),type);
            break;
        case this.COLLECTD_CONST:
            this.CollectdForm.show(this.getMetaData(element),type);
            break;
        case this.CONSOLE_CONST:
            this.ConsoleForm.show(this.getMetaData(element),type);
            break;
        }
    };
    
    /**
     * redis面板设置组件元数据
     */
    MQRocketMQPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.VBROKER_CONST:
        case this.BROKER_CONST:
        case this.NAMESRV_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.COLLECTD_CONST:
        case this.CONSOLE_CONST:
            delete data.INST_ID;
            delete data.POS;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        }
        element._id = id;
        element.text = name;
        element.meta = data; //metadata
    }
    
    /**
     * 提取组件元数据
     */
    MQRocketMQPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.VBROKER_CONST:
        case this.BROKER_CONST:
        case this.NAMESRV_CONST:
            data.INST_ID = element._id;
            break;
        case this.COLLECTD_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.collectd.x;
            pos.y = this.collectd.y;
            data.POS = pos;
            break;
        case this.CONSOLE_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.RocketMQConsole.x;
            pos.y = this.RocketMQConsole.y;
            data.POS = pos;
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
    MQRocketMQPlate.prototype.getElementDeployed = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "1";
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.deployedMenu.show(e);
            });
        } else if (element.elementType == "container") {
            if (element.parentContainer != undefined && element.parentContainer != null) {
                element.removeEventListener('contextmenu');
                element.addEventListener('contextmenu', function(e) {
                    self.elementMenu.show(e);
                });
            }
        }
    };
    
    /**
     * 缓存面板卸载
     */
    MQRocketMQPlate.prototype.undeployElement = function(element){
        if("COLLECTD" == element.type){
            Component.Plate.prototype.undeployElement.call(this, element);
        }else{
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "已经是最后一个组件！");
            } else {
                Component.Plate.prototype.undeployElement.call(this, element);
            }
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    MQRocketMQPlate.prototype.getElementUndeployed = function(element) {
        if (element.elementType == "node") {
            element.status = "0";
            var self = this;
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.nodeMenu.show(e);
            });
        }
    };
    
    MQRocketMQPlate.prototype.addRocketMQConsole = function (x, y, img, text, type, menu, isSaved) {
        if (this.rmqConsole != null) {
            Component.Alert("warn", "集群中只能有一个console！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.RocketMQConsole = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }

})(Component);
