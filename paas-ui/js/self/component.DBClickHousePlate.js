var Component = window.Component || {};


(function(Component) {
    
    /**
     * ClickHouse面板类
     */
    function DBClickHousePlate(url, id, name, canvas, isProduct) {
        this.loadSchema("db.clickhouse.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "DB_CLICKHOUSE";
        
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
        this.ClickHouseIcon = "clickhouse.png";
        this.ZookeeperIcon = "zookkeeper.png";
        
        //常量
        this.CLICKHOUSE_REPLICAS_CONST = "CLICKHOUSE_REPLICAS";
        this.CLICKHOUSE_SERVER_CONST = "CLICKHOUSE_SERVER";
        this.ZOO_KEEPER_CONST = "ZOOKEEPER";
        
        this.showMetaDataOnMouse = false;
        
        this.ClickHouseReplicasContainer = null;
        this.ZooKeeperContainer = null;
        Util.hideLoading();
        var self = this;
        
        this.elementMenu =$.contextMenu({
            items:[
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
                   }}
               ]
        });
        
        this.deployedMenu = $.contextMenu({
            items:[
                {
                    label:'查看信息', icon:'../images/console/icon_delete.png', callback: function(e) { self.popupForm(e.target,"view"); }
                },
                {
                    label:'卸载(缩容)', icon:'../images/console/icon_delete.png', callback: function(e) { self.undeployElement(e.target);}
                },
                {
                    label:'删除组件', icon:'../images/console/icon_delete.png', callback: function(e){
                       var element = e.target;
                       layer.confirm("确认删除组件吗？", {
                           btn: ['是','否'], //按钮
                           title: "确认"
                       }, function(){
                           layer.close(layer.index);
                           self.deleteComponentBackground(element);
                       });
                   }
                }
            ]
        });
        
        //初始化弹出表单
        var cancelFunction = function() {
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        
        this.ClickHouseReplicasForm = $.popupForm(this.CLICKHOUSE_REPLICAS_CONST, window['db.clickhouse.schema'], function(json) {
            self.saveElementData(self.popElement, json, self.ClickHouseReplicasForm);
        }, cancelFunction);
        
        this.ClickHouseForm = $.popupForm(this.CLICKHOUSE_SERVER_CONST, window['db.clickhouse.schema'], function(json) {
            self.saveElementData(self.popElement, json, self.ClickHouseForm);
        }, cancelFunction);
        
        this.ZookeeperForm = $.popupForm(this.ZOO_KEEPER_CONST, window['db.clickhouse.schema'], function(json) {
            self.saveElementData(self.popElement, json, self.ZookeeperForm);
        }, cancelFunction);
        
        this.PrometheusForm = $.popupForm(this.PROMETHEUS_CONST, window['db.clickhouse.schema'], function(json){
            self.saveElementData(self.popElement, json, self.PrometheusForm);
        }, cancelFunction);
        
        this.GrafanaForm = $.popupForm(this.GRAFANA_CONST, window['db.clickhouse.schema'], function(json){
            self.saveElementData(self.popElement, json, self.GrafanaForm);
        }, cancelFunction);
        
        this.getSSHList("DB", [this.ClickHouseForm, this.ZookeeperForm, this.PrometheusForm, this.GrafanaForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    DBClickHousePlate.prototype = new Component.Plate();
    Component.DBClickHousePlate = DBClickHousePlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    DBClickHousePlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.CLICKHOUSE_SERV_CONTAINER;
            this.needInitTopo = false;

            var CLICKHOUSE_REPLICAS_CONTAINER = topoData.CLICKHOUSE_REPLICAS_CONTAINER;
            var ZOOKEEPER_CONTAINER = topoData.ZOOKEEPER_CONTAINER;
            var PROMETHEUS = topoData.PROMETHEUS;
            var GRAFANA = topoData.GRAFANA;

            this.ClickHouseReplicasContainer = this.makeContainer(
                CLICKHOUSE_REPLICAS_CONTAINER.POS.x, CLICKHOUSE_REPLICAS_CONTAINER.POS.y,
                'clickhouse-replicas容器',
                CLICKHOUSE_REPLICAS_CONTAINER.POS.row, CLICKHOUSE_REPLICAS_CONTAINER.POS.col, "container");
            this.ClickHouseReplicasContainer._id = CLICKHOUSE_REPLICAS_CONTAINER.INST_ID;

            this.ZooKeeperContainer = this.makeContainer(
                ZOOKEEPER_CONTAINER.POS.x, ZOOKEEPER_CONTAINER.POS.y,
                'zookeeper容器', 
                ZOOKEEPER_CONTAINER.POS.row, ZOOKEEPER_CONTAINER.POS.col, "container");
            this.ZooKeeperContainer._id = ZOOKEEPER_CONTAINER.INST_ID;
            
            for (var i=0; i<CLICKHOUSE_REPLICAS_CONTAINER.CLICKHOUSE_REPLICAS.length; i++) {
                var clickhouseReplicas = CLICKHOUSE_REPLICAS_CONTAINER.CLICKHOUSE_REPLICAS[i];
                var container = this.addContainerToContainer(CLICKHOUSE_REPLICAS_CONTAINER.POS.x, CLICKHOUSE_REPLICAS_CONTAINER.POS.y,
                    'replicas', this.CLICKHOUSE_REPLICAS_CONST, 1, 2, this.elementMenu, this.ClickHouseReplicasContainer, true);
                
                for (var j = 0; j < clickhouseReplicas.CLICKHOUSE_SERVER.length; j++) {
                    var clickhouse = clickhouseReplicas.CLICKHOUSE_SERVER[j];
                    var node = this.addNodeToContainer(container.x +1, container.y +1, this.iconDir+this.ClickHouseIcon, 
                        "clickhouse", this.CLICKHOUSE_SERVER_CONST, this.nodeMenu, container, true, false, STATUS_UNDEPLOYED);
                    this.setMetaData(node, clickhouse);
                }
                this.setMetaData(container, clickhouseReplicas);
            }

            for (var i=0; i<ZOOKEEPER_CONTAINER.ZOOKEEPER.length; i++) {
                var zookeeper = ZOOKEEPER_CONTAINER.ZOOKEEPER[i];
                var node = this.addNodeToContainer(this.ZooKeeperContainer.x+1,
                    this.ZooKeeperContainer.y+1, this.iconDir+this.ZookeeperIcon,
                    zookeeper.INST_ID, this.ZOO_KEEPER_CONST, this.nodeMenu,
                    this.ZooKeeperContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, zookeeper);
            }
            
            if (PROMETHEUS && !$.isEmptyObject(PROMETHEUS)) {
                var x = PROMETHEUS.POS ? PROMETHEUS.POS.x : 0;
                var y = PROMETHEUS.POS ? PROMETHEUS.POS.y : 0;
                this.addPrometheus(x, y, this.iconDir+this.prometheusIcon,
                        PROMETHEUS.INST_ID, this.PROMETHEUS_CONST, this.nodeMenu, true),
                this.setMetaData(this.prometheus, PROMETHEUS);
            }
            
            if (GRAFANA && !$.isEmptyObject(GRAFANA)) {
                var x = GRAFANA.POS ? GRAFANA.POS.x : 0;
                var y = GRAFANA.POS ? GRAFANA.POS.y : 0;
                this.addGrafana(x, y, this.iconDir+this.grafanaIcon,
                        GRAFANA.INST_ID, this.GRAFANA_CONST, this.nodeMenu, true),
                this.setMetaData(this.grafana, GRAFANA);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.ZooKeeperContainer = this.makeContainer(this.width*0.4, this.height*0.2, "zookeeper 容器", 1, 2, "container");
            this.ClickHouseReplicasContainer = this.makeContainer(this.width*0.4, this.height*0.6, "clickhouse-replicas 容器", 1, 2, "container");
        }

        //添加container连接
        link = new JTopo.FlexionalLink(this.ZooKeeperContainer, this.ClickHouseReplicasContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    DBClickHousePlate.prototype.toPlateJson = function() {
        var CLICKHOUSE_SERV_CONTAINER = {};
        CLICKHOUSE_SERV_CONTAINER.INST_ID = this.id;

        //clickhouse
        var CLICKHOUSE_REPLICAS_CONTAINER = {};
        CLICKHOUSE_REPLICAS_CONTAINER.INST_ID = this.ClickHouseReplicasContainer._id;
        CLICKHOUSE_REPLICAS_CONTAINER.POS = this.ClickHouseReplicasContainer.getPosJson();
        var CLICKHOUSE_REPLICAS = [];
        CLICKHOUSE_REPLICAS_CONTAINER.CLICKHOUSE_REPLICAS = CLICKHOUSE_REPLICAS;
        CLICKHOUSE_SERV_CONTAINER.CLICKHOUSE_REPLICAS_CONTAINER = CLICKHOUSE_REPLICAS_CONTAINER;

        //zookeeper
        var ZOOKEEPER_CONTAINER = {};
        ZOOKEEPER_CONTAINER.INST_ID = this.ZooKeeperContainer._id;
        ZOOKEEPER_CONTAINER.POS = this.ZooKeeperContainer.getPosJson();
        var ZOOKEEPER = [];
        ZOOKEEPER_CONTAINER.ZOOKEEPER = ZOOKEEPER;
        CLICKHOUSE_SERV_CONTAINER.ZOOKEEPER_CONTAINER = ZOOKEEPER_CONTAINER;
        
        var prometheus = {};
        if (this.prometheus != null) {
            prometheus.INST_ID = this.prometheus._id;
            prometheus.SSH_ID = this.prometheus.SSH_ID;
            prometheus.PROMETHEUS_PORT = this.prometheus.PROMETHEUS_PORT;
            
            var pos = {};
            pos.x = this.prometheus.x+this.prometheus.width/2;
            pos.y = this.prometheus.y+this.prometheus.height/2;
            prometheus.POS = pos;
            
            CLICKHOUSE_SERV_CONTAINER.PROMETHEUS = prometheus;
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
            
            CLICKHOUSE_SERV_CONTAINER.GRAFANA = grafana;
        }
        
        return {"CLICKHOUSE_SERV_CONTAINER": CLICKHOUSE_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    DBClickHousePlate.prototype.newComponent = function(x, y, datatype) {
        switch(datatype) {
        case this.CLICKHOUSE_REPLICAS_CONST:
            return this.addContainerToContainer(x, y, 'clickhouse-replicas', this.CLICKHOUSE_REPLICAS_CONST, 1, 2, this.elementMenu, this.ClickHouseReplicasContainer, false) != null;
        case this.CLICKHOUSE_SERVER_CONST:
            var childs = this.ClickHouseReplicasContainer.childs;
            var success;
            for (var i = 0; i<childs.length; i++) {
                var replicas = childs[i];
                if (!replicas.isInContainer(x, y)) {
                    continue;
                }
                
                if(this.addNodeToContainer(x, y, this.iconDir+this.ClickHouseIcon,
                    "clickhouse", datatype, this.nodeMenu, replicas, false, true, STATUS_NEW) != null){
                    
                    success = true;
                    break;
                }
            }
            return success;
        case this.ZOO_KEEPER_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.ZookeeperIcon, 
                "zookeeper", datatype, this.nodeMenu, this.ZooKeeperContainer, false, true, STATUS_NEW) != null;
        case this.PROMETHEUS_CONST:
            return this.addPrometheus(x, y, this.iconDir+this.prometheusIcon, "prometheus", datatype, this.nodeMenu, false, STATUS_NEW);
        case this.GRAFANA_CONST:
            return this.addGrafana(x, y, this.iconDir+this.grafanaIcon, "grafana", datatype, this.nodeMenu, false, STATUS_NEW);
        }
    };
    
    /**
     * 弹出窗口
     */
    DBClickHousePlate.prototype.popupForm = function(element,type) {
        this.popElement = element;
        switch(element.type) {
        case this.CLICKHOUSE_REPLICAS_CONST:
            this.ClickHouseReplicasForm.show(this.getMetaData(element), type);
            break;
        case this.CLICKHOUSE_SERVER_CONST:
            this.ClickHouseForm.show(this.getMetaData(element), type);
            break;
        case this.ZOO_KEEPER_CONST:
            this.ZookeeperForm.show(this.getMetaData(element), type);
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
    DBClickHousePlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.CLICKHOUSE_REPLICAS_CONST:
        case this.CLICKHOUSE_SERVER_CONST:
        case this.ZOO_KEEPER_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
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
    DBClickHousePlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.CLICKHOUSE_REPLICAS_CONST:
        case this.CLICKHOUSE_SERVER_CONST:
        case this.ZOO_KEEPER_CONST:
            data.INST_ID = element._id;
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
    DBClickHousePlate.prototype.getElementDeployed = function(element) {
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
    DBClickHousePlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.CLICKHOUSE_SERVER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "clickhouse-server的数量不能小于1！");
                res = false;
            }
            break;
        case this.ZOO_KEEPER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "zookeeper的数量不能小于1！");
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
    DBClickHousePlate.prototype.getElementUndeployed = function(element) {
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
