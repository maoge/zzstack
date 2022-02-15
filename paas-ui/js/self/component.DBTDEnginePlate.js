var Component = window.Component || {};


(function(Component) {
    
    /**
     * 缓存面板类
     */
    function DBTDEnginePlate(url, id, name, canvas , isProduct) {
        this.loadSchema("db.tdengine.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "DB_TDENGINE";
        //调用父类方法初始化舞台
        var data = null;
        this.initStage(id, name, canvas,isProduct);
        data = this.getTopoData(id);
        if (data == null) {
            Util.hideLoading();
            return;
        } else if (data == "init") {
            data = null;
        }
        //图标(暂定)
        this.DnodeIcon = "dnode-node.png";
        this.ArbitratorIcon = "arbitrator-node.png";
        this.collectdIcon = "collectd_icon.png";
        
        //常量
        this.DNODE_CONST = "TD_DNODE";
        this.ARBITRATOR_CONST = "TD_ARBITRATOR";
        
        this.showMetaDataOnMouse = false;
        
        this.ArbitratorContainer = null;
        this.DnodeContainer = null;
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
        this.DnodeForm = $.popupForm(this.DNODE_CONST, window['db.tdengine.schema'], function(json){
            self.saveElementData(self.popElement, json, self.DnodeForm);
        }, cancelFunction);
        this.ArbitratorForm = $.popupForm(this.ARBITRATOR_CONST, window['db.tdengine.schema'], function(json){

            self.saveElementData(self.popElement, json, self.ArbitratorForm);
        }, cancelFunction);
        this.CollectdForm = $.popupForm(this.COLLECTD_CONST, window['db.tdengine.schema'], function(json){
            self.saveElementData(self.popElement, json, self.CollectdForm);
        }, cancelFunction);
        this.getSSHList("DB", [ this.DnodeForm, this.ArbitratorForm, this.CollectdForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    DBTDEnginePlate.prototype = new Component.Plate();
    Component.DBTDEnginePlate = DBTDEnginePlate;
    
    /**
     * 初始化Arbitrator、DnodeContainer 集群container
     */
    DBTDEnginePlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.TDENGINE_SERV_CONTAINER;
            this.needInitTopo = false;

            var ARBITRATOR_CONTAINER = topoData.ARBITRATOR_CONTAINER;
            var DNODE_CONTAINER = topoData.DNODE_CONTAINER;
            var collectd = topoData.COLLECTD;

            this.DnodeContainer = this.makeContainer(
                DNODE_CONTAINER.POS.x, DNODE_CONTAINER.POS.y,
                'dnode容器', 
                DNODE_CONTAINER.POS.row, DNODE_CONTAINER.POS.col, "container");
            this.DnodeContainer._id = DNODE_CONTAINER.INST_ID;

            this.ArbitratorContainer = this.makeContainer(
                ARBITRATOR_CONTAINER.POS.x, ARBITRATOR_CONTAINER.POS.y,
                'arbitrator容器',
                ARBITRATOR_CONTAINER.POS.row, ARBITRATOR_CONTAINER.POS.col, "container");
            this.ArbitratorContainer._id = ARBITRATOR_CONTAINER.INST_ID;

            for (var i=0; i<DNODE_CONTAINER.TD_DNODE.length; i++) {
                var dnode = DNODE_CONTAINER.TD_DNODE[i];
                var node = this.addNodeToContainer(this.DnodeContainer.x+1,
                    this.DnodeContainer.y+1, this.iconDir+this.DnodeIcon,
                    dnode.INST_ID, this.DNODE_CONST, this.nodeMenu,
                    this.DnodeContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, dnode);
            }

            var arbitrator = ARBITRATOR_CONTAINER.TD_ARBITRATOR;
            var node = this.addNodeToContainer(this.ArbitratorContainer.x+1, 
                this.ArbitratorContainer.y+1, this.iconDir+this.ArbitratorIcon, 
                arbitrator.INST_ID, this.ARBITRATOR_CONST, this.nodeMenu, 
                this.ArbitratorContainer, true, false, STATUS_UNDEPLOYED);
            this.setMetaData(node, arbitrator);

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
            this.DnodeContainer = this.makeContainer(this.width*0.5, this.height*0.2, "dnode容器", 1, 2, "container");
            this.ArbitratorContainer = this.makeContainer(this.width*0.5, this.height*0.6, "arbitrator容器", 1, 2, "container");

        }

        //添加container连接
        link = new JTopo.FlexionalLink( this.DnodeContainer, this.ArbitratorContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    DBTDEnginePlate.prototype.toPlateJson = function(needCollectd) {
        var TDENGINE_SERV_CONTAINER = {};
        TDENGINE_SERV_CONTAINER.INST_ID = this.id;
        
         //dnode集群
         var DNODE_CONTAINER = {};
         DNODE_CONTAINER.INST_ID = this.DnodeContainer._id;
         DNODE_CONTAINER.POS = this.DnodeContainer.getPosJson();
         var TD_DNODE = [];
         DNODE_CONTAINER.TD_DNODE = TD_DNODE;
         TDENGINE_SERV_CONTAINER.DNODE_CONTAINER = DNODE_CONTAINER;

        //arbitrator
        var ARBITRATOR_CONTAINER = {};
        ARBITRATOR_CONTAINER.INST_ID = this.ArbitratorContainer._id;
        ARBITRATOR_CONTAINER.POS = this.ArbitratorContainer.getPosJson();
        var TD_ARBITRATOR = {};
        ARBITRATOR_CONTAINER.TD_ARBITRATOR = TD_ARBITRATOR;
        TDENGINE_SERV_CONTAINER.ARBITRATOR_CONTAINER = ARBITRATOR_CONTAINER;  
        
        //当第一次保存面板或collectd为空时，不需要传collectd信息
        var collectd = {};
        if (needCollectd && this.collectd != null) {
            collectd.INST_ID = this.collectd._id;
            var pos = {};
            pos.x = this.collectd.x+this.collectd.width/2;
            pos.y = this.collectd.y+this.collectd.height/2;
            collectd.POS = pos;
        }
        TDENGINE_SERV_CONTAINER.COLLECTD = collectd;
        
        return {"TDENGINE_SERV_CONTAINER": TDENGINE_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    DBTDEnginePlate.prototype.newComponent = function(x, y, datatype) {
        // var container, img, text;
        switch(datatype) {
        case this.ARBITRATOR_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.ArbitratorIcon, 
                    "arbitrator", datatype, this.nodeMenu, this.ArbitratorContainer, false, true, STATUS_NEW) != null;
        case this.DNODE_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.DnodeIcon, 
                    "dnode", datatype, this.nodeMenu, this.DnodeContainer, false, true, STATUS_NEW) != null;
        case this.COLLECTD_CONST:
            return this.addCollectd(x, y, this.iconDir+this.collectdIcon, "collectd", datatype, this.nodeMenu, false, STATUS_NEW);
        }
    };
    
    /**
     * 弹出窗口
     */
    DBTDEnginePlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.ARBITRATOR_CONST:
            this.ArbitratorForm.show(this.getMetaData(element),type);
            break;
        case this.DNODE_CONST:
            this.DnodeForm.show(this.getMetaData(element),type);
            break;
        case this.COLLECTD_CONST:
            this.CollectdForm.show(this.getMetaData(element),type);
            break;
        }
    };
    
    /**
     * redis面板设置组件元数据
     */
    DBTDEnginePlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;

        switch(element.type) {
        case this.ARBITRATOR_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.DNODE_CONST:
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
    DBTDEnginePlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.ARBITRATOR_CONST:
            data.INST_ID = element._id;
            break;
        case this.DNODE_CONST:
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
    DBTDEnginePlate.prototype.getElementDeployed = function(element) {
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
    DBTDEnginePlate.prototype.undeployElement = function(element){
        if(element.parentContainer.childs.length <= 1 ){
            Component.Alert("error", "已经是最后一个组件！");
        } else {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    DBTDEnginePlate.prototype.getElementUndeployed = function(element) {
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
