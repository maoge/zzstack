var Component = window.Component || {};


(function(Component) {
    
    /**
     * 缓存面板类
     */
    function CacheRedisMSPlate(url, id, name, canvas) {
        this.loadSchema("cache.redis.ms.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "CACHE_REDIS_MASTER_SLAVE";
        this.overridePlateMenu = true;
        
        //调用父类方法初始化舞台
        var data = null;
        this.initStage(id, name, canvas);
        data = this.getTopoData(id);
        if (data == null) {
            Util.hideLoading();
            return;
        } else if (data == "init") {
            data = null;
        }

        //图标(暂定)
        this.NodeIcon = "redis-node.png";
        this.collectdIcon = "collectd_icon.png";
        
        //常量
        this.NODE_CONST = "REDIS_NODE";
        
        this.showMetaDataOnMouse = false;
        
        this.NodeContainer = null;
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
                            self.deployElement(e.target,"1");
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
        this.nodeMenu = $.contextMenu({
            items:[
                   {label:'部署组件', icon:'../images/console/icon_install.png', callback: function(e){
                       self.deployElement(e.target);
                   }},
                   {label:'修改信息', icon:'../images/console/icon_edit.png', callback: function(e){        
                       self.popupForm(e.target,"view");
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
        
        this.NodeForm = $.popupForm(this.NODE_CONST, window['cache.redis.ms.schema'],function(json){
            self.saveElementData(self.popElement, json, self.NodeForm);
        }, cancelFunction);
        this.CollectdForm = $.popupForm(this.COLLECTD_CONST, window['cache.redis.ms.schema'], function(json){
            self.saveElementData(self.popElement, json, self.CollectdForm);
        }, cancelFunction);
        this.getSSHList("CACHE", [this.NodeForm, this.CollectdForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    CacheRedisMSPlate.prototype = new Component.Plate();
    Component.CacheRedisMSPlate = CacheRedisMSPlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    CacheRedisMSPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.REDIS_SERV_MS_CONTAINER;
            this.needInitTopo = false;

            var REDIS_NODE_CONTAINER = topoData.REDIS_NODE_CONTAINER;
            var collectd = topoData.COLLECTD;
            
            this.NodeContainer = this.makeContainer(
                REDIS_NODE_CONTAINER.POS.x, REDIS_NODE_CONTAINER.POS.y,
                'redis server容器', 
                REDIS_NODE_CONTAINER.POS.row, REDIS_NODE_CONTAINER.POS.col, "container");
            this.NodeContainer._id = REDIS_NODE_CONTAINER.INST_ID;
            
            for (var i=0; i<REDIS_NODE_CONTAINER.REDIS_NODE.length; i++) {
                var cacheNode = REDIS_NODE_CONTAINER.REDIS_NODE[i];
                var node = this.addNodeToContainer(this.NodeContainer.x+1,
                    this.NodeContainer.y+1, this.iconDir+this.NodeIcon,
                    cacheNode.INST_ID, this.NODE_CONST, this.nodeMenu,
                    this.NodeContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, cacheNode);
            }

            if (collectd && !$.isEmptyObject(collectd)) {
                var x = collectd.POS ? collectd.POS.x : 0;
                var y = collectd.POS ? collectd.POS.y : 0;
                this.addCollectd(x, y, this.iconDir+this.collectdIcon, 
                        collectd.INST_ID, this.COLLECTD_CONST, this.nodeMenu, true, STATUS_UNDEPLOYED),
                this.setMetaData(this.collectd, collectd);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.NodeContainer = this.makeContainer(this.width*0.5, this.height*0.6, "Redis实例集群", 1, 2, "container");
        }

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    CacheRedisMSPlate.prototype.toPlateJson = function(needCollectd) {
        var REDIS_SERV_MS_CONTAINER = {};
        REDIS_SERV_MS_CONTAINER.INST_ID = this.id;
        
        //redis集群
        var REDIS_NODE_CONTAINER = {};
        REDIS_NODE_CONTAINER.INST_ID = this.NodeContainer._id;
        REDIS_NODE_CONTAINER.POS = this.NodeContainer.getPosJson();
        var REDIS_NODE = [];
        REDIS_NODE_CONTAINER.REDIS_NODE = REDIS_NODE;
        REDIS_SERV_MS_CONTAINER.REDIS_NODE_CONTAINER = REDIS_NODE_CONTAINER;
        
        //当第一次保存面板或collectd为空时，不需要传collectd信息
        var collectd = {};
        if (needCollectd && this.collectd != null) {
            collectd.INST_ID = this.collectd._id;
            var pos = {};
            pos.x = this.collectd.x+this.collectd.width/2;
            pos.y = this.collectd.y+this.collectd.height/2;
            collectd.POS = pos;
        }
        REDIS_SERV_MS_CONTAINER.COLLECTD = collectd;
        
        return {"REDIS_SERV_MS_CONTAINER": REDIS_SERV_MS_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    CacheRedisMSPlate.prototype.newComponent = function(x, y, datatype) {
        // var container, img, text;
        switch(datatype) {
        case this.NODE_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.NodeIcon, 
                "redis", datatype, this.nodeMenu, this.NodeContainer, false, true, STATUS_NEW) != null;
        case this.COLLECTD_CONST:
            return this.addCollectd(x, y, this.iconDir+this.collectdIcon, "collectd", datatype, this.nodeMenu, false, STATUS_NEW);
        }
    };
    
    /**
     * 弹出窗口
     */
    CacheRedisMSPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.NODE_CONST:
            this.NodeForm.show(this.getMetaData(element),type);
            break;
        case this.COLLECTD_CONST:
            this.CollectdForm.show(this.getMetaData(element),type);
            break;
        }
    };
    
    /**
     * redis面板设置组件元数据
     */
    CacheRedisMSPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.NODE_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.COLLECTD_CONST:
            //id = data.INST_ID;
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
    CacheRedisMSPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.NODE_CONST:
            data.INST_ID = element._id;
            break;
        case this.COLLECTD_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.collectd.x;
            pos.y = this.collectd.y;
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
    CacheRedisMSPlate.prototype.getElementDeployed = function(element) {
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
    CacheRedisMSPlate.prototype.undeployElement = function(element){
        if(element.parentContainer.childs.length <= 1 ){
            Component.Alert("error", "已经是最后一个组件！");
        } else {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    CacheRedisMSPlate.prototype.getElementUndeployed = function(element) {
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
