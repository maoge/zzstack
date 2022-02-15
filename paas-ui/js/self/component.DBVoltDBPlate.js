var Component = window.Component || {};


(function(Component) {
    
    /**
     * VoltDB面板类
     */
    function DBVoltDBPlate(url, id, name, canvas, isProduct) {
        this.loadSchema("db.voltdb.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "DB_VOLTDB";
        
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
        this.VoltDBIcon = "voltdb-server.png";
        
        //常量
        this.VOLTDB_CONTAINER_CONST = "VOLTDB_CONTAINER";
        this.VOLTDB_SERVER_CONST = "VOLTDB_SERVER";
        
        this.showMetaDataOnMouse = false;
        
        this.VoltDBContainer = null;
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
                        var data = self.VoltDBContainer.childs;
                        if(data.length>1){
                            for(var i=0; i<data.length; i++){
                                if(data[i]._id == element._id){
                                    if(data[i].childs.length>0){
                                        Component.Alert("warn","内部存在组件，无法直接删除；请将内部组件全部删除，再进行此操作！");
                                        return
                                    }
                                }       
                            }
                        }
                        self.deleteComponentBackground(element);
                    });
                }}
            ]
        });
        
        //初始化右键菜单
        this.nodeMenu = $.contextMenu({
            items:[
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
        
        var res = window['db.voltdb.schema'];
        this.VoltDBForm = $.popupForm(this.VOLTDB_SERVER_CONST, res, function(json) {
            self.saveElementData(self.popElement, json, self.VoltDBForm);
        }, cancelFunction);
        
        this.getSSHList("DB", [this.VoltDBForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    DBVoltDBPlate.prototype = new Component.Plate();
    Component.DBVoltDBPlate = DBVoltDBPlate;
    
    /**
     * 初始化container
     */
    DBVoltDBPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.VOLTDB_SERV_CONTAINER;
            this.needInitTopo = false;

            var VOLTDB_CONTAINER = topoData.VOLTDB_CONTAINER;

            this.VoltDBContainer = this.makeContainer(
                VOLTDB_CONTAINER.POS.x, VOLTDB_CONTAINER.POS.y,
                'voltdb容器',
                VOLTDB_CONTAINER.POS.row, VOLTDB_CONTAINER.POS.col, "container");
            this.VoltDBContainer._id = VOLTDB_CONTAINER.INST_ID;

            for (var i=0; i < VOLTDB_CONTAINER.VOLTDB_SERVER.length; i++) {
                var voltdb = VOLTDB_CONTAINER.VOLTDB_SERVER[i];
                var node = this.addNodeToContainer(this.VoltDBContainer.x+1,
                    this.VoltDBContainer.y+1, this.iconDir+this.VoltDBIcon,
                    voltdb.INST_ID, this.VOLTDB_SERVER_CONST, this.nodeMenu,
                    this.VoltDBContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, voltdb);
            }

            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.VoltDBContainer = this.makeContainer(this.width*0.4, this.height*0.4, "voltdb容器", 1, 2, "container");
        }

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    DBVoltDBPlate.prototype.toPlateJson = function() {
        var VOLTDB_SERV_CONTAINER = {};
        VOLTDB_SERV_CONTAINER.INST_ID = this.id;

        var VOLTDB_CONTAINER = {};
        VOLTDB_CONTAINER.INST_ID = this.VoltDBContainer._id;
        VOLTDB_CONTAINER.POS = this.VoltDBContainer.getPosJson();
        
        VOLTDB_CONTAINER.VOLTDB_SERVER = [];
        VOLTDB_SERV_CONTAINER.VOLTDB_CONTAINER = VOLTDB_CONTAINER;

        return {"VOLTDB_SERV_CONTAINER": VOLTDB_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    DBVoltDBPlate.prototype.newComponent = function(x, y, datatype) {
        switch(datatype) {
        case this.VOLTDB_SERVER_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.VoltDBIcon, 
                "voltdb-server", datatype, this.nodeMenu, this.VoltDBContainer, false, true, STATUS_NEW) != null;
        }
    };
    
    /**
     * 弹出窗口
     */
    DBVoltDBPlate.prototype.popupForm = function(element,type) {
        this.popElement = element;
        switch(element.type) {
        case this.VOLTDB_SERVER_CONST:
            this.VoltDBForm.show(this.getMetaData(element), type);
            break;
        }
    };
    
    /**
     * 面板设置组件元数据
     */
    DBVoltDBPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.VOLTDB_SERVER_CONST:
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
        element.meta = data; //metadata
    }
    
    /**
     * 提取组件元数据
     */
    DBVoltDBPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.VOLTDB_SERVER_CONST:
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
    DBVoltDBPlate.prototype.getElementDeployed = function(element) {
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
    DBVoltDBPlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.VOLTDB_SERVER_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "voltdb-server的数量不能小于1！");
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
    DBVoltDBPlate.prototype.getElementUndeployed = function(element) {
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
