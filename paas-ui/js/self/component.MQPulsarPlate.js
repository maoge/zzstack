var Component = window.Component || {};


(function(Component) {
    
    /**
     * 缓存面板类
     */
    function MQPulsarPlate(url, id, name, canvas, isProduct) {
        this.loadSchema("mq.pulsar.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "MQ_PULSAR";
        
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
        this.BrokerIcon = "broker.png";
        this.BookkeeperIcon = "bookkeeper.png";
        this.ZookeeperIcon = "zookkeeper.png";
        this.PulsarManagerIcon = "pulsar-manager.png";
        
        //常量
        this.BROKER_CONST = "PULSAR_BROKER";
        this.BOOKKEEPER_CONST = "PULSAR_BOOKKEEPER";
        this.ZOO_KEEPER_CONST = "ZOOKEEPER";
        
        this.showMetaDataOnMouse = false;
        
        this.BrokerContainer = null;
        this.BookkeeperContainer = null;
        this.ZooKeeperContainer = null;
        
        Util.hideLoading();
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
        
        this.BrokerForm = $.popupForm(this.BROKER_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.BrokerForm);
        }, cancelFunction);
        this.BookkeeperForm = $.popupForm(this.BOOKKEEPER_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.BookkeeperForm);
        }, cancelFunction);
        this.ZookeeperForm = $.popupForm(this.ZOO_KEEPER_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.ZookeeperForm);
        }, cancelFunction);
        this.PulsarManagerForm = $.popupForm(this.PULSAR_MANAGER_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.PulsarManagerForm);
        }, cancelFunction);
        this.PrometheusForm = $.popupForm(this.PROMETHEUS_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.PrometheusForm);
        }, cancelFunction);
        this.GrafanaForm = $.popupForm(this.GRAFANA_CONST, window['mq.pulsar.schema'], function(json){
            self.saveElementData(self.popElement, json, self.GrafanaForm);
        }, cancelFunction);
        this.getSSHList("MQ", [this.BrokerForm, this.BookkeeperForm, this.ZookeeperForm, this.PulsarManagerForm, this.PrometheusForm, this.GrafanaForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    MQPulsarPlate.prototype = new Component.Plate();
    Component.MQPulsarPlate = MQPulsarPlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    MQPulsarPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.PULSAR_SERV_CONTAINER;
            this.needInitTopo = false;

            var PULSAR_BROKER_CONTAINER = topoData.PULSAR_BROKER_CONTAINER;
            var PULSAR_BOOKKEEPER_CONTAINER = topoData.PULSAR_BOOKKEEPER_CONTAINER;
            var ZOOKEEPER_CONTAINER = topoData.ZOOKEEPER_CONTAINER;
            var PULSAR_MANAGER = topoData.PULSAR_MANAGER;
            var PROMETHEUS = topoData.PROMETHEUS;
            var GRAFANA = topoData.GRAFANA;
            
            this.BrokerContainer = this.makeContainer(
                PULSAR_BROKER_CONTAINER.POS.x, PULSAR_BROKER_CONTAINER.POS.y,
                'pulsar broker容器',
                PULSAR_BROKER_CONTAINER.POS.row, PULSAR_BROKER_CONTAINER.POS.col, "container");
            this.BrokerContainer._id = PULSAR_BROKER_CONTAINER.INST_ID;
            
            this.BookkeeperContainer = this.makeContainer(
                PULSAR_BOOKKEEPER_CONTAINER.POS.x, PULSAR_BOOKKEEPER_CONTAINER.POS.y,
                'bookkeeper容器', 
                PULSAR_BOOKKEEPER_CONTAINER.POS.row, PULSAR_BOOKKEEPER_CONTAINER.POS.col, "container");
            this.BookkeeperContainer._id = PULSAR_BOOKKEEPER_CONTAINER.INST_ID;
            
            this.ZooKeeperContainer = this.makeContainer(
                ZOOKEEPER_CONTAINER.POS.x, ZOOKEEPER_CONTAINER.POS.y,
                'zookeeper容器', 
                ZOOKEEPER_CONTAINER.POS.row, ZOOKEEPER_CONTAINER.POS.col, "container");
            this.ZooKeeperContainer._id = ZOOKEEPER_CONTAINER.INST_ID;

            for (var i=0; i<PULSAR_BROKER_CONTAINER.PULSAR_BROKER.length; i++) {
                var broker = PULSAR_BROKER_CONTAINER.PULSAR_BROKER[i];
                var node = this.addNodeToContainer(this.BrokerContainer.x+1, 
                    this.BrokerContainer.y+1, this.iconDir+this.BrokerIcon, 
                    broker.INST_ID, this.BROKER_CONST, this.nodeMenu, 
                    this.BrokerContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, broker);
            }
            
            for (var i=0; i<PULSAR_BOOKKEEPER_CONTAINER.PULSAR_BOOKKEEPER.length; i++) {
                var bookkeeper = PULSAR_BOOKKEEPER_CONTAINER.PULSAR_BOOKKEEPER[i];
                var node = this.addNodeToContainer(this.BookkeeperContainer.x+1,
                    this.BookkeeperContainer.y+1, this.iconDir+this.BookkeeperIcon,
                    bookkeeper.INST_ID, this.BOOKKEEPER_CONST, this.nodeMenu,
                    this.BookkeeperContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, bookkeeper);
            }

            for (var i=0; i<ZOOKEEPER_CONTAINER.ZOOKEEPER.length; i++) {
                var zookeeper = ZOOKEEPER_CONTAINER.ZOOKEEPER[i];
                var node = this.addNodeToContainer(this.ZooKeeperContainer.x+1,
                    this.ZooKeeperContainer.y+1, this.iconDir+this.ZookeeperIcon,
                    zookeeper.INST_ID, this.ZOO_KEEPER_CONST, this.nodeMenu,
                    this.ZooKeeperContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, zookeeper);
            }
            
            if (PULSAR_MANAGER && !$.isEmptyObject(PULSAR_MANAGER)) {
                var x = PULSAR_MANAGER.POS ? PULSAR_MANAGER.POS.x : 0;
                var y = PULSAR_MANAGER.POS ? PULSAR_MANAGER.POS.y : 0;
                this.addPulsarManager(x, y, this.iconDir+this.PulsarManagerIcon, 
                        PULSAR_MANAGER.INST_ID, this.PULSAR_MANAGER_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED),
                this.setMetaData(this.PulsarManager, PULSAR_MANAGER);
            }
            
            if (PROMETHEUS && !$.isEmptyObject(PROMETHEUS)) {
                var x = PROMETHEUS.POS ? PROMETHEUS.POS.x : 0;
                var y = PROMETHEUS.POS ? PROMETHEUS.POS.y : 0;
                this.addPrometheus(x, y, this.iconDir+this.prometheusIcon, 
                        PROMETHEUS.INST_ID, this.PROMETHEUS_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED),
                this.setMetaData(this.prometheus, PROMETHEUS);
            }
            
            if (GRAFANA && !$.isEmptyObject(GRAFANA)) {
                var x = GRAFANA.POS ? GRAFANA.POS.x : 0;
                var y = GRAFANA.POS ? GRAFANA.POS.y : 0;
                this.addGrafana(x, y, this.iconDir+this.grafanaIcon, 
                        GRAFANA.INST_ID, this.GRAFANA_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED),
                this.setMetaData(this.grafana, GRAFANA);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.BrokerContainer = this.makeContainer(this.width*0.4, this.height*0.2, "broker 容器", 1, 2, "container");
            this.BookkeeperContainer = this.makeContainer(this.width*0.4, this.height*0.6, "bookkeeper 容器", 1, 2, "container");
            this.ZooKeeperContainer = this.makeContainer(this.width*0.7, this.height*0.4, "zookeeper 容器", 1, 2, "container");
        } 

        //添加container连接
        link = new JTopo.FlexionalLink(this.BrokerContainer, this.BookkeeperContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        link = new JTopo.FlexionalLink(this.BookkeeperContainer, this.ZooKeeperContainer);
        link.direction = 'horizontal';
        this.scene.add(link);

        link = new JTopo.FlexionalLink(this.ZooKeeperContainer, this.BrokerContainer);
        link.direction = 'horizontal';
        this.scene.add(link);

        Util.hideLoading();
    }
    
    //新增prometheus
    MQPulsarPlate.prototype.addPulsarManager = function (x, y, img, text, type, menu, isSaved, status) {
        if (this.PulsarManager != null) {
            Component.Alert("warn", "集群中只能有一个pulsar_manager！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.PulsarManager = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    MQPulsarPlate.prototype.toPlateJson = function() {
        var PULSAR_SERV_CONTAINER = {};
        PULSAR_SERV_CONTAINER.INST_ID = this.id;
        
        //broker
        var PULSAR_BROKER_CONTAINER = {};
        PULSAR_BROKER_CONTAINER.INST_ID = this.BrokerContainer._id;
        PULSAR_BROKER_CONTAINER.POS = this.BrokerContainer.getPosJson();
        var PULSAR_BROKER = [];
        PULSAR_BROKER_CONTAINER.PULSAR_BROKER = PULSAR_BROKER;
        PULSAR_SERV_CONTAINER.PULSAR_BROKER_CONTAINER = PULSAR_BROKER_CONTAINER;
        
        //bookkeeper
        var PULSAR_BOOKKEEPER_CONTAINER = {};
        PULSAR_BOOKKEEPER_CONTAINER.INST_ID = this.BookkeeperContainer._id;
        PULSAR_BOOKKEEPER_CONTAINER.POS = this.BookkeeperContainer.getPosJson();
        var PULSAR_BOOKKEEPER = [];
        PULSAR_BOOKKEEPER_CONTAINER.PULSAR_BOOKKEEPER = PULSAR_BOOKKEEPER;
        PULSAR_SERV_CONTAINER.PULSAR_BOOKKEEPER_CONTAINER = PULSAR_BOOKKEEPER_CONTAINER;
        
        //zookeeper
        var ZOOKEEPER_CONTAINER = {};
        ZOOKEEPER_CONTAINER.INST_ID = this.ZooKeeperContainer._id;
        ZOOKEEPER_CONTAINER.POS = this.ZooKeeperContainer.getPosJson();
        var ZOOKEEPER = [];
        ZOOKEEPER_CONTAINER.ZOOKEEPER = ZOOKEEPER;
        PULSAR_SERV_CONTAINER.ZOOKEEPER_CONTAINER = ZOOKEEPER_CONTAINER;
        
        var manager = {};
        if (this.PulsarManager != null) {
            manager.INST_ID = this.PulsarManager._id;
            manager.SSH_ID = this.PulsarManager.SSH_ID;
            manager.PULSAR_MGR_PORT = this.PulsarManager.PULSAR_MGR_PORT;
            manager.HERDDB_PORT = this.PulsarManager.HERDDB_PORT;
            
            var pos = {};
            pos.x = this.PulsarManager.x+this.PulsarManager.width/2;
            pos.y = this.PulsarManager.y+this.PulsarManager.height/2;
            manager.POS = pos;
            
            PULSAR_SERV_CONTAINER.PULSAR_MANAGER = manager;
        }

        var prometheus = {};
        if (this.prometheus != null) {
            prometheus.INST_ID = this.prometheus._id;
            prometheus.SSH_ID = this.prometheus.SSH_ID;
            prometheus.PROMETHEUS_PORT = this.prometheus.PROMETHEUS_PORT;
            
            var pos = {};
            pos.x = this.prometheus.x+this.prometheus.width/2;
            pos.y = this.prometheus.y+this.prometheus.height/2;
            prometheus.POS = pos;
            
            PULSAR_SERV_CONTAINER.PROMETHEUS = prometheus;
        }
        
        var grafana = {};
        if (this.grafana != null) {
            grafana.INST_ID = this.grafana._id;
            grafana.SSH_ID = this.grafana.SSH_ID;
            grafana.HTTP_PORT = this.grafana.HTTP_PORT;
            
            var pos = {};
            pos.x = this.grafana.x+this.grafana.width/2;
            pos.y = this.grafana.y+this.grafana.height/2;
            grafana.POS = pos;
            
            PULSAR_SERV_CONTAINER.GRAFANA = grafana;
        }
        
        return {"PULSAR_SERV_CONTAINER": PULSAR_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    MQPulsarPlate.prototype.newComponent = function(x, y, datatype) {
        switch(datatype) {
        case this.BROKER_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.BrokerIcon, 
                    "broker", datatype, this.nodeMenu, this.BrokerContainer, false, true, STATUS_NEW) != null;
        case this.BOOKKEEPER_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.BookkeeperIcon, 
                "bookkeeper", datatype, this.nodeMenu, this.BookkeeperContainer, false, true, STATUS_NEW) != null;
        case this.ZOO_KEEPER_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.ZookeeperIcon, 
                "zookeeper", datatype, this.nodeMenu, this.ZooKeeperContainer, false, true, STATUS_NEW) != null;
        case this.PULSAR_MANAGER_CONST:
            return this.addPulsarManager(x, y, this.iconDir+this.PulsarManagerIcon, "pulsar manager", datatype, this.nodeMenu, false, STATUS_NEW);
        case this.PROMETHEUS_CONST:
            return this.addPrometheus(x, y, this.iconDir+this.prometheusIcon, "prometheus", datatype, this.nodeMenu, false, STATUS_NEW);
        case this.GRAFANA_CONST:
            return this.addGrafana(x, y, this.iconDir+this.grafanaIcon, "grafana", datatype, this.nodeMenu, false, STATUS_NEW);
        }
    };
    
    /**
     * 弹出窗口
     */
    MQPulsarPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.BROKER_CONST:
            this.BrokerForm.show(this.getMetaData(element),type);
            break;
        case this.BOOKKEEPER_CONST:
            this.BookkeeperForm.show(this.getMetaData(element),type);
            break;
        case this.ZOO_KEEPER_CONST:
            this.ZookeeperForm.show(this.getMetaData(element),type);
            break;
        case this.PULSAR_MANAGER_CONST:
            this.PulsarManagerForm.show(this.getMetaData(element),type);
            break;
        case this.PROMETHEUS_CONST:
            this.PrometheusForm.show(this.getMetaData(element),type);
            break;
        case this.GRAFANA_CONST:
            this.GrafanaForm.show(this.getMetaData(element),type);
            break;
        }
    };
    
    /**
     * 面板设置组件元数据
     */
    MQPulsarPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.BROKER_CONST:
        case this.BOOKKEEPER_CONST:
        case this.ZOO_KEEPER_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.PULSAR_MANAGER_CONST:
        case this.PROMETHEUS_CONST:
        case this.GRAFANA_CONST:
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
    MQPulsarPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.BROKER_CONST:
        case this.BOOKKEEPER_CONST:
        case this.ZOO_KEEPER_CONST:
            data.INST_ID = element._id;
            break;
        case this.PULSAR_MANAGER_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.PulsarManager.x;
            pos.y = this.PulsarManager.y;
            data.POS = pos;
            break;
        case this.PROMETHEUS_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.prometheus.x;
            pos.y = this.prometheus.y;
            data.POS = pos;
            break;
        case this.GRAFANA_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.grafana.x;
            pos.y = this.grafana.y;
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
    MQPulsarPlate.prototype.getElementDeployed = function(element) {
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
                    self.deployedMenu.show(e);
                });
            }
        }
    };
    
    /**
     * 面板卸载
     */
    MQPulsarPlate.prototype.undeployElement = function(element){
        /*if(element.parentContainer.childs.length <= 1 ){
            Component.Alert("error", "已经是最后一个组件！");
        } else {
            Component.Plate.prototype.undeployElement.call(this, element);
        }*/
        
        var res = true;
        switch (element.type) {
        case this.BROKER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "pulsar broker的数量不能小于1！");
                res = false;
            }
            break;
        case this.BOOKKEEPER_CONST:
            if(element.parentContainer.childs.length <= 3 ){
                Component.Alert("error", "bookkeeper的数量不能小于3！");
                res = false;
            }
            break;
        case this.ZOO_KEEPER_CONST:
            if(element.parentContainer.childs.length <= 3 ){
                Component.Alert("error", "zookeeper的数量不能小于3！");
                res = false;
            }
            break;
        }

        if (res) {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    MQPulsarPlate.prototype.getElementUndeployed = function(element) {
        if (element.elementType == "node") {
            element.status = "0";
            var self = this;
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.nodeMenu.show(e);
            });
        }
    };
    
})(Component);
