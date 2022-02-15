var Component = window.Component || {};


(function(Component) {
    
    /**
     * apisix面板
     */
    function ServerlessAPISixPlate(url, id, name, canvas,isDeployed) {
        this.loadSchema("serverless.apisix.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "SERVERLESS_APISIX";
        
        //调用父类方法初始化舞台
        var data = null;
        this.initStage(id, name, canvas,isDeployed);
        data = this.getTopoData(id);
        if (data == null) {
            Util.hideLoading();
            return;
        } else if (data == "init") {
            data = null;
        }

        //图标(暂定)
        this.apisixIcon = "apisix.png";
        this.etcdIcon = "apisix-etcd-node.png";
        this.collectdIcon = "collectd_icon.png";
        
        //常量
        this.APISIX_CONST = "APISIX_SERVER";
        this.ETCD_CONST = "ETCD";
        
        this.showMetaDataOnMouse = false;
        
        this.ApisixContainer = null;
        this.ApisixEtcdContainer = null;
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
                }}
            ]
        });
        this.deployedMenu = $.contextMenu({
            items:[
                {label:'查看信息', icon:'../images/console/icon_delete.png', callback: function(e){
                    self.popupForm(e.target,"view");
                }},
                {label:'卸载(缩容)', icon:'../images/console/icon_delete.png', callback: function(e){
                    self.undeployElement(e.target);
                }}
            ]
        });
        
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        
        this.ApisixForm = $.popupForm(this.APISIX_CONST, window['serverless.apisix.schema'], function(json){
            self.saveElementData(self.popElement, json, self.ApisixForm);
        }, cancelFunction);
        this.EtcdForm = $.popupForm(this.ETCD_CONST, window['serverless.apisix.schema'], function(json){
            self.saveElementData(self.popElement, json, self.EtcdForm);
        }, cancelFunction);
        this.CollectdForm = $.popupForm(this.COLLECTD_CONST, window['serverless.apisix.schema'], function(json){
            self.saveElementData(self.popElement, json, self.CollectdForm);
        }, cancelFunction);
        this.PrometheusForm = $.popupForm(this.PROMETHEUS_CONST, window['serverless.apisix.schema'], function(json){
            self.saveElementData(self.popElement, json, self.PrometheusForm);
        }, cancelFunction);
        this.GrafanaForm = $.popupForm(this.GRAFANA_CONST, window['serverless.apisix.schema'], function(json){
            self.saveElementData(self.popElement, json, self.GrafanaForm);
        }, cancelFunction);
        
        this.getSSHList("SERVERLESS", [this.ApisixForm, this.EtcdForm, this.CollectdForm, this.PrometheusForm, this.GrafanaForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    ServerlessAPISixPlate.prototype = new Component.Plate();
    Component.ServerlessAPISixPlate = ServerlessAPISixPlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    ServerlessAPISixPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.APISIX_SERV_CONTAINER;
            this.needInitTopo = false;

            var APISIX_CONTAINER = topoData.APISIX_CONTAINER;
            var ETCD_CONTAINER = topoData.ETCD_CONTAINER;
            var PROMETHEUS = topoData.PROMETHEUS;
            var GRAFANA = topoData.GRAFANA;
            var collectd = topoData.COLLECTD;
            
            this.ApisixContainer = this.makeContainer(
                APISIX_CONTAINER.POS.x, APISIX_CONTAINER.POS.y,
                'apisix容器',
                APISIX_CONTAINER.POS.row, APISIX_CONTAINER.POS.col, "container");
            this.ApisixContainer._id = APISIX_CONTAINER.INST_ID;
            
            this.ApisixEtcdContainer = this.makeContainer(
                ETCD_CONTAINER.POS.x, ETCD_CONTAINER.POS.y,
                'etcd容器', 
                ETCD_CONTAINER.POS.row, ETCD_CONTAINER.POS.col, "container");
            this.ApisixEtcdContainer._id = ETCD_CONTAINER.INST_ID;
            
            for (var i=0; i<APISIX_CONTAINER.APISIX_SERVER.length; i++) {
                var apisix = APISIX_CONTAINER.APISIX_SERVER[i];
                var node = this.addNodeToContainer(this.ApisixContainer.x+1, 
                    this.ApisixContainer.y+1, this.iconDir+this.apisixIcon, 
                    apisix.INST_ID, this.APISIX_CONST, this.nodeMenu, 
                    this.ApisixContainer, true, false);
                this.setMetaData(node, apisix);
            }
            
            for (var i=0; i<ETCD_CONTAINER.ETCD.length; i++) {
                var etcd = ETCD_CONTAINER.ETCD[i];
                var node = this.addNodeToContainer(this.ApisixEtcdContainer.x+1,
                    this.ApisixEtcdContainer.y+1, this.iconDir+this.etcdIcon,
                    etcd.INST_ID, this.ETCD_CONST, this.nodeMenu,
                    this.ApisixEtcdContainer, true, false);
                this.setMetaData(node, etcd);
            }

            if (collectd && !$.isEmptyObject(collectd)) {
                var x = collectd.POS ? collectd.POS.x : 0;
                var y = collectd.POS ? collectd.POS.y : 0;
                this.addCollectd(x, y, this.iconDir+this.collectdIcon, 
                        collectd.INST_ID, this.COLLECTD_CONST, this.nodeMenu, true),
                this.setMetaData(this.collectd, collectd);
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
            this.ApisixContainer = this.makeContainer(this.width*0.5, this.height*0.2, "apisix容器", 1, 2, "container");
            this.ApisixEtcdContainer = this.makeContainer(this.width*0.5, this.height*0.6, "etcd容器", 1, 2, "container");
        }

        //添加container连接
        link = new JTopo.FlexionalLink(this.ApisixContainer, this.ApisixEtcdContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    ServerlessAPISixPlate.prototype.toPlateJson = function(needCollectd) {
        var APISIX_SERV_CONTAINER = {};
        APISIX_SERV_CONTAINER.INST_ID = this.id;
        
        //apisix
        var APISIX_CONTAINER = {};
        APISIX_CONTAINER.INST_ID = this.ApisixContainer._id;
        APISIX_CONTAINER.POS = this.ApisixContainer.getPosJson();
        var APISIX_SERVER = [];
        APISIX_CONTAINER.APISIX_SERVER = APISIX_SERVER;
        APISIX_SERV_CONTAINER.APISIX_CONTAINER = APISIX_CONTAINER;
        
        //etcd
        var ETCD_CONTAINER = {};
        ETCD_CONTAINER.INST_ID = this.ApisixEtcdContainer._id;
        ETCD_CONTAINER.POS = this.ApisixEtcdContainer.getPosJson();
        var ETCD = [];
        ETCD_CONTAINER.ETCD = ETCD;
        APISIX_SERV_CONTAINER.ETCD_CONTAINER = ETCD_CONTAINER;
        
        //当第一次保存面板或collectd为空时，不需要传collectd信息
        var collectd = {};
        if (needCollectd && this.collectd != null) {
            collectd.INST_ID = this.collectd._id;
            var pos = {};
            pos.x = this.collectd.x+this.collectd.width/2;
            pos.y = this.collectd.y+this.collectd.height/2;
            collectd.POS = pos;
            
            APISIX_SERV_CONTAINER.COLLECTD = collectd;
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
            
            APISIX_SERV_CONTAINER.PROMETHEUS = prometheus;
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
            
            APISIX_SERV_CONTAINER.GRAFANA = grafana;
        }
        
        return {"APISIX_SERV_CONTAINER": APISIX_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    ServerlessAPISixPlate.prototype.newComponent = function(x, y, datatype) {
        // var container, img, text;
        switch(datatype) {
        case this.APISIX_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.apisixIcon, 
                    "apisix", datatype, this.nodeMenu, this.ApisixContainer, false, true) != null;
        case this.ETCD_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.etcdIcon, 
                    "etcd", datatype, this.nodeMenu, this.ApisixEtcdContainer, false, true) != null;
        case this.COLLECTD_CONST:
            return this.addCollectd(x, y, this.iconDir+this.collectdIcon, "collectd", datatype, this.nodeMenu, false);
        case this.PROMETHEUS_CONST:
            return this.addPrometheus(x, y, this.iconDir+this.prometheusIcon, "prometheus", datatype, this.nodeMenu, false);
        case this.GRAFANA_CONST:
            return this.addGrafana(x, y, this.iconDir+this.grafanaIcon, "grafana", datatype, this.nodeMenu, false);
        }
    };
    
    /**
     * 弹出窗口
     */
    ServerlessAPISixPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.APISIX_CONST:
            this.ApisixForm.show(this.getMetaData(element),type);
            break;
        case this.ETCD_CONST:
            this.EtcdForm.show(this.getMetaData(element),type);
            break;
        case this.COLLECTD_CONST:
            this.CollectdForm.show(this.getMetaData(element),type);
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
     * redis面板设置组件元数据
     */
    ServerlessAPISixPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.APISIX_CONST:
        case this.ETCD_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.COLLECTD_CONST:
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
    ServerlessAPISixPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.APISIX_CONST:
        case this.ETCD_CONST:
            data.INST_ID = element._id;
            break;
        case this.COLLECTD_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.collectd.x;
            pos.y = this.collectd.y;
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
    ServerlessAPISixPlate.prototype.getElementDeployed = function(element) {
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
     * 缓存面板卸载
     */
    ServerlessAPISixPlate.prototype.undeployElement = function(element){
        if(element.parentContainer.childs.length <= 1 ){
            Component.Alert("error", "已经是最后一个组件！");
        } else {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    ServerlessAPISixPlate.prototype.getElementUndeployed = function(element) {
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
