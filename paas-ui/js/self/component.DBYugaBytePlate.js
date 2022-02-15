var Component = window.Component || {};


(function(Component) {
    
    /**
     * YugaByteDB面板类
     */
    function DBYugaBytePlate(url, id, name, canvas, isProduct) {
        this.loadSchema("db.yugabyte.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "DB_YUGABYTEDB";
        
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
        this.YbMasterIcon = "yb-master.png";
        this.YbTServerIcon = "yb-tserver.png";
        
        //常量
        this.YB_MASTER_CONTAINER_CONST = "YB_MASTER_CONTAINER";
        this.YB_TSERVER_CONTAINER_CONST = "YB_TSERVER_CONTAINER";
        
        this.YB_MASTER_CONST = "YB_MASTER";
        this.YB_TSERVER_CONST = "YB_TSERVER";
        
        this.showMetaDataOnMouse = false;
        
        this.YbMasterContainer = null;
        this.YbTServerContainer = null;
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
        this.MasterForm = $.popupForm(this.YB_MASTER_CONST, window['db.yugabyte.schema'], function(json){
            self.saveElementData(self.popElement, json, self.MasterForm);
        }, cancelFunction);
        this.TServerForm = $.popupForm(this.YB_TSERVER_CONST, window['db.yugabyte.schema'], function(json){
            self.saveElementData(self.popElement, json, self.TServerForm);
        }, cancelFunction);
        
        this.getSSHList("DB", [this.MasterForm, this.TServerForm]);

        //初始化Container
        this.initContainer(data);
    }

    DBYugaBytePlate.prototype = new Component.Plate();
    Component.DBYugaBytePlate = DBYugaBytePlate;

    /**
     * 初始化container及各个容器的下属实例
     */
    DBYugaBytePlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.YUGABYTEDB_SERV_CONTAINER;
            this.needInitTopo = false;

            var YB_MASTER_CONTAINER = topoData.YB_MASTER_CONTAINER;
            var YB_TSERVER_CONTAINER = topoData.YB_TSERVER_CONTAINER;

            this.YbMasterContainer = this.makeContainer(YB_MASTER_CONTAINER.POS.x, YB_MASTER_CONTAINER.POS.y, "yb-master集群", 
                    YB_MASTER_CONTAINER.POS.row, YB_MASTER_CONTAINER.POS.col, "container");
            this.YbMasterContainer._id = YB_MASTER_CONTAINER.INST_ID;

            this.YbTServerContainer = this.makeContainer(YB_TSERVER_CONTAINER.POS.x, YB_TSERVER_CONTAINER.POS.y, "yb-tserver集群",
                    YB_TSERVER_CONTAINER.POS.row, YB_TSERVER_CONTAINER.POS.col, "container");
            this.YbTServerContainer._id = YB_TSERVER_CONTAINER.INST_ID;
            
            for (var i = 0; i < YB_MASTER_CONTAINER.YB_MASTER.length; i++) {
                var ybMaster = YB_MASTER_CONTAINER.YB_MASTER[i];
                var node = this.addNodeToContainer(this.YbMasterContainer.x+1, this.YbMasterContainer.y+1,
                        this.iconDir+this.YbMasterIcon, ybMaster.INST_ID,
                        this.YB_MASTER_CONST, this.nodeMenu, 
                        this.YbMasterContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, ybMaster);
            }
                
            for (var i = 0; i < YB_TSERVER_CONTAINER.YB_TSERVER.length; i++) {
                var ybTserver = YB_TSERVER_CONTAINER.YB_TSERVER[i];
                var node = this.addNodeToContainer(this.YbTServerContainer.x+1, this.YbTServerContainer.y+1, 
                        this.iconDir+this.YbTServerIcon, ybTserver.INST_ID,
                        this.YB_TSERVER_CONST, this.nodeMenu,
                        this.YbTServerContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, ybTserver);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.YbMasterContainer = this.makeContainer(this.width*0.4, this.height*0.2, "yb-master集群", 1, 1, "container");
            this.YbTServerContainer = this.makeContainer(this.width*0.4, this.height*0.6, "yb-tserver集群", 1, 1, "container");
        }
            
        //添加container连接
        var link = new JTopo.FlexionalLink(this.YbMasterContainer, this.YbTServerContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }

    // 保存面板拓扑信息(位置信息等)
    DBYugaBytePlate.prototype.toPlateJson = function(_needCollectd) {
        var YUGABYTEDB_SERV_CONTAINER = {};
        YUGABYTEDB_SERV_CONTAINER.INST_ID = this.id;
        
        //Master集群
        var YB_MASTER_CONTAINER = {};
        YB_MASTER_CONTAINER.INST_ID = this.YbMasterContainer._id;
        YB_MASTER_CONTAINER.POS = this.YbMasterContainer.getPosJson();
        var YB_MASTER = [];
        YB_MASTER_CONTAINER.YB_MASTER = YB_MASTER;
        YUGABYTEDB_SERV_CONTAINER.YB_MASTER_CONTAINER = YB_MASTER_CONTAINER;
        
        //TServer集群
        var YB_TSERVER_CONTAINER = {};
        YB_TSERVER_CONTAINER.INST_ID = this.YbTServerContainer._id;
        YB_TSERVER_CONTAINER.POS = this.YbTServerContainer.getPosJson();
        var YB_TSERVER = [];
        YB_TSERVER_CONTAINER.YB_TSERVER = YB_TSERVER;
        YUGABYTEDB_SERV_CONTAINER.YB_TSERVER_CONTAINER = YB_TSERVER_CONTAINER;
        
        return {"YUGABYTEDB_SERV_CONTAINER": YUGABYTEDB_SERV_CONTAINER};
    }
        
    // yugabyte面板新增组件
    DBYugaBytePlate.prototype.newComponent = function(x, y, datatype) {
        var container, img, text;
        switch(datatype) {
        case this.YB_MASTER_CONST:
            container = this.YbMasterContainer;
            img = this.iconDir+this.YbMasterIcon;
            text = "YB_MASTER";
            break;
        case this.YB_TSERVER_CONST:
            container = this.YbTServerContainer;
            img = this.iconDir+this.YbTServerIcon;
            text = "YB_TSERVER";
            break;
        }
        return this.addNodeToContainer(x, y, img, text, datatype, this.nodeMenu, container, false, true, STATUS_NEW) != null;
    }

    // 弹出窗口
    DBYugaBytePlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.YB_MASTER_CONST:
            this.MasterForm.show(this.getMetaData(element),type);
            break;
        case this.YB_TSERVER_CONST:
            this.TServerForm.show(this.getMetaData(element),type);
            break;
        }
    }
        
    // yugabyte面板设置组件元数据
    DBYugaBytePlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;

        switch(element.type) {
        case this.YB_MASTER_CONST:
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.YB_TSERVER_CONST:
            delete data.INST_ID;
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
        element.meta = data;
    }
        
    // 提取组件元数据
    DBYugaBytePlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.YB_MASTER_CONST:
        case this.YB_TSERVER_CONST:
            data.INST_ID = element._id;
            break;
        }
        for (var i in element.meta) {
            data[i] = element.meta[i];
        }
        return data;
    }

    // 组件部署成功时的处理
    DBYugaBytePlate.prototype.getElementDeployed = function(element) {
        if (element.elementType == "node") {
            element.status = "1";
            var self = this;
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

    // yugabyte卸载元素
    DBYugaBytePlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.YB_MASTER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "yb-master的数量不能小于1！");
                res = false;
            }
            break;
        case this.YB_TSERVER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "yb-tserver的数量不能小于1！");
                res = false;
            }
            break;
        }

        if (res) {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    }

    // 组件卸载成功时的处理
    DBYugaBytePlate.prototype.getElementUndeployed = function(element) {
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
