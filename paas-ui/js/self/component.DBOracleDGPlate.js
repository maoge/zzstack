var Component = window.Component || {};


(function(Component) {
    /**
     * OracleDG面板
     */
    function DBOracleDGPlate(url, id, name, canvas, isProduct, showPateMenu) {
        this.loadSchema("db.oracledg.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "DB_ORACLE_DG";
        
        this.overridePlateMenu =true;
        this.showMetaDataOnMouse = false;
        this.showPlateMenu = showPateMenu;
        
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
        this.OracleIcon = "oracle-node.png";
        
        //常量
        this.ORACLE_CONST = "ORCL_INSTANCE";
        this.ORACLE_CONTAINER_CONST = "DG_CONTAINER"

        this.MetadbContainer = null;
        this.RealDB1Container = null;
        this.RealDB2Container = null;
        this.RealDB3Container = null;
        this.RealDB4Container = null;


        Util.hideLoading();
        var self = this;
        //初始化右键菜单

        this.nodeMenu = $.contextMenu({
            items:[
                //    {label:'部署组件', icon:'../images/console/icon_install.png', callback: function(e){
                //        self.deployElement(e.target);
                //    }},
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
                    }}
                ]
        });
        this.plateMenu = $.contextMenu({
            items:[
                {label:'保存面板结构', icon:'../images/console/icon_save.png', callback: function(_e){
                        self.saveTopoData();
                    }},
                {label:'伪部署面板', icon:'../images/console/icon_pseudo_deployment.png', callback: function(e){
                    layer.confirm('确认要伪部署集群'+self.name+'”吗？', {
                        btn: ['是','否'], //按钮
                        title: "确认"
                    }, function(){
                        layer.close(layer.index);
                        self.deployElement(e.target, 2);  
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
        this.deployeContainerMenu =$.contextMenu({
            items:[
                {label:'主从切换', icon:'../images/console/icon_save.png', callback: function(e){
                    var containerName = e.target.text;
                    if(e.target.childs.length>=2){
                        var data = self.getTopoData(id)
                        self.changeMasterSlave(e.target, data, plate.smsId, self.id, containerName)
                    }else{
                        Component.Alert("warn", "存在两个节点的时候才可以切换！");
                    }  
                }},
              
            ]
        });
        this.noDeployeContainerMenu =$.contextMenu({
            items:[
                {label:'编辑信息', icon:'../images/console/icon_edit.png', callback: function(e){ 
                    e.target.type=self.ORACLE_CONTAINER_CONST;
                    self.popupForm(e.target,"edit");
                }}
            ]
        });
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        
        this.OracleForm = $.popupForm(this.ORACLE_CONST, window['db.oracledg.schema'],function(json){
            self.saveElementData(self.popElement, json, self.OracleForm);
        }, cancelFunction);

        this.OracleContainerForm = $.popupForm(this.ORACLE_CONTAINER_CONST, window['db.oracledg.schema'],function(json){
            self.saveElementData(self.popElement, json, self.OracleContainerForm);
        }, cancelFunction);
        
        this.getSSHList("DB", [this.OracleForm, this.OracleContainerForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    DBOracleDGPlate.prototype = new Component.Plate();
    Component.DBOracleDGPlate = DBOracleDGPlate;
    
    /**
     * 初始化container
     */
    DBOracleDGPlate.prototype.initContainer = function(data) {
        var self = this;
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.ORACLE_DG_SERV_CONTAINER;
            this.needInitTopo = false;
            
            var DG_CONTAINER = topoData.DG_CONTAINER; 

            for (var i=0; i<DG_CONTAINER.length; i++) {
                var item = DG_CONTAINER[i];
                var dgName = item.DG_NAME;
                var pos = item.POS;
                var orclInstances = item.ORCL_INSTANCE;
                var container = this.makeContainer(pos.x, pos.y, dgName, pos.row, pos.col, "container");
                container._id = item.INST_ID;
                if (dgName == "metadb") {
                    this.MetadbContainer = container;
                } else if (dgName == "realdb1") {
                    this.RealDB1Container = container;
                } else if (dgName == "realdb2") {
                    this.RealDB2Container = container;
                } else if (dgName == "realdb3") {
                    this.RealDB3Container = container;
                } else if (dgName == "realdb4") {
                    this.RealDB4Container = container;
                }
                this.addOrclInstance(container, orclInstances, item.ACTIVE_DB_TYPE, STATUS_UNDEPLOYED);
               
            }
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true; 
            this.MetadbContainer = this.makeContainer(this.width*0.1, this.height*0.4, "metadb", 1, 1, "container");
            this.RealDB1Container = this.makeContainer(this.width*0.3, this.height*0.4, "realdb1", 1, 1, "container");
            this.RealDB2Container = this.makeContainer(this.width*0.5, this.height*0.4, "realdb2", 1, 1, "container");
            this.RealDB3Container = this.makeContainer(this.width*0.7, this.height*0.4, "realdb3", 1, 1, "container");
            this.RealDB4Container = this.makeContainer(this.width*0.9, this.height*0.4, "realdb4", 1, 1, "container");
        }
        this.MetadbContainer.addEventListener('contextmenu', function(e) {
            if(plate.smsId){
                self.deployeContainerMenu.show(e);
            }
            if(!self.isDeployedPlate()){
                self.noDeployeContainerMenu.show(e);
            }
        });
        this.RealDB1Container.addEventListener('contextmenu', function(e) {
            if(plate.smsId){
                self.deployeContainerMenu.show(e);
            }  
            if(!self.isDeployedPlate()){
                self.noDeployeContainerMenu.show(e);
            }
        });
        this.RealDB2Container.addEventListener('contextmenu', function(e) {
            if(plate.smsId){
                self.deployeContainerMenu.show(e);
            }
            if(!self.isDeployedPlate()){
                self.noDeployeContainerMenu.show(e);
            }
        });
        this.RealDB3Container.addEventListener('contextmenu', function(e) {
            if(plate.smsId){
                self.deployeContainerMenu.show(e);
            } 
            if(!self.isDeployedPlate()){
                self.noDeployeContainerMenu.show(e);
            }
        });
        this.RealDB4Container.addEventListener('contextmenu', function(e) {
            if(plate.smsId){
                self.deployeContainerMenu.show(e);
            }
            if(!self.isDeployedPlate()){
                self.noDeployeContainerMenu.show(e);
            }
        });
        Util.hideLoading();
    }
    
    DBOracleDGPlate.prototype.addOrclInstance = function(dgContainer, orclIntances, isPitch, status) {
        for (var i=0; i<orclIntances.length; i++) {
            var instance = orclIntances[i];
            var node = this.addNodeToContainer(dgContainer.x+1, 
                    dgContainer.y+1, this.iconDir+this.OracleIcon, 
                    instance.INST_ID, this.ORACLE_CONST, this.nodeMenu, 
                    dgContainer, true, isPitch, status, instance.NODE_TYPE);
            
            this.setMetaData(node, instance);
        }
    }

    // 判断面板是否部署,来决定容器编辑菜单是否显示
    DBOracleDGPlate.prototype.isDeployedPlate =function(){
        var self = this;
        var data = self.getTopoData(self.id);
        if(data == null || data == "init"){
            return true;
        }
        if(data && data != 'init'){
            Util.showLoading();
            self.scene.clear();
            self.initContainer(data);
            Util.hideLoading();
            if(data.DEPLOY_FLAG.length>0){
                for(var i=0; i<data.DEPLOY_FLAG.length; i++){
                    var isDeployedValue = data.DEPLOY_FLAG[i][self.id];
                    if(Number(isDeployedValue)){
                        return true;
                    }else{
                        continue;
                    }
                }
            }
        }
        return false
    }
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    DBOracleDGPlate.prototype.toPlateJson = function(_needCollectd) {
        var ORACLE_DG_SERV_CONTAINER = {};
        ORACLE_DG_SERV_CONTAINER.INST_ID = this.id;
        //oracle
        var DG_CONTAINER = new Array(5);
        
        var METADB_CONTAINER = {};
        METADB_CONTAINER.INST_ID = this.MetadbContainer._id;
        METADB_CONTAINER.DG_NAME = this.MetadbContainer.text;
        METADB_CONTAINER.ACTIVE_DB_TYPE = "master";
        var metadbContainerPos = this.MetadbContainer.getPosJson();
        METADB_CONTAINER.POS = metadbContainerPos;

        var REALDB1_CONTAINER = {};
        REALDB1_CONTAINER.INST_ID = this.RealDB1Container._id;
        REALDB1_CONTAINER.DG_NAME = this.RealDB1Container.text;
        REALDB1_CONTAINER.ACTIVE_DB_TYPE = "master";
        var realdb1ContainerPos = this.RealDB1Container.getPosJson();
        REALDB1_CONTAINER.POS = realdb1ContainerPos;
        
        var REALDB2_CONTAINER = {};
        REALDB2_CONTAINER.INST_ID = this.RealDB2Container._id;
        REALDB2_CONTAINER.DG_NAME = this.RealDB2Container.text;
        REALDB2_CONTAINER.ACTIVE_DB_TYPE = "master";
        var realdb2ContainerPos = this.RealDB2Container.getPosJson();
        REALDB2_CONTAINER.POS = realdb2ContainerPos;
        
        var REALDB3_CONTAINER = {};
        REALDB3_CONTAINER.INST_ID = this.RealDB3Container._id;
        REALDB3_CONTAINER.DG_NAME = this.RealDB3Container.text;
        REALDB3_CONTAINER.ACTIVE_DB_TYPE = "master";
        var realdb3ContainerPos = this.RealDB3Container.getPosJson();
        REALDB3_CONTAINER.POS = realdb3ContainerPos;
        
        var REALDB4_CONTAINER = {};
        REALDB4_CONTAINER.INST_ID = this.RealDB4Container._id;
        REALDB4_CONTAINER.DG_NAME = this.RealDB4Container.text;
        REALDB4_CONTAINER.ACTIVE_DB_TYPE = "master";
        var realdb4ContainerPos = this.RealDB4Container.getPosJson();
        REALDB4_CONTAINER.POS = realdb4ContainerPos;
        
        DG_CONTAINER[0] = METADB_CONTAINER;
        DG_CONTAINER[1] = REALDB1_CONTAINER;
        DG_CONTAINER[2] = REALDB2_CONTAINER;
        DG_CONTAINER[3] = REALDB3_CONTAINER;
        DG_CONTAINER[4] = REALDB4_CONTAINER;

        ORACLE_DG_SERV_CONTAINER.DG_CONTAINER = DG_CONTAINER;
        return {"ORACLE_DG_SERV_CONTAINER": ORACLE_DG_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    DBOracleDGPlate.prototype.newComponent = function(x, y, datatype) {
        switch(datatype) {
        case this.ORACLE_CONST:
            return this.addNewOrcl(x, y, datatype, STATUS_NEW);
        }
    };
    
    DBOracleDGPlate.prototype.addNewOrcl = function(x, y, datatype, status) {
        var container = this.getDGContainer(x, y);
        if (container != null) {
            return this.addNodeToContainer(x, y, this.iconDir+this.OracleIcon, "oracle", datatype, this.nodeMenu, container, false, false, status) != null;
        } else {
            return false;
        }
    };
    
    DBOracleDGPlate.prototype.getDGContainer = function(x, y) {
        if (this.MetadbContainer.isInContainer(x, y)) {
            return this.MetadbContainer;
        } else if (this.RealDB1Container.isInContainer(x, y)) {
            return this.RealDB1Container;
        } else if (this.RealDB2Container.isInContainer(x, y)) {
            return this.RealDB2Container;
        } else if (this.RealDB3Container.isInContainer(x, y)) {
            return this.RealDB3Container;
        } else if (this.RealDB4Container.isInContainer(x, y)) {
            return this.RealDB4Container;
        } else {
            return null;
        }
    };
    
    /**
     * 弹出窗口
     */
    DBOracleDGPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.ORACLE_CONST:
            this.OracleForm.show(this.getMetaData(element),type);
            break;
        case this.ORACLE_CONTAINER_CONST:
            this.OracleContainerForm.show(this.getMetaData(element),type);
            break;
        }
    };
    
    /**
     * redis面板设置组件元数据
     */
    DBOracleDGPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.ORACLE_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.ORACLE_CONTAINER_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
        }
        if(element.elementType == "node"){
            if(data.NODE_TYPE == "1"){
                element.text = "M";
            }else if(data.NODE_TYPE == "0"){
                element.text = "S";
            }
        }
        element._id = id;
        element.meta = data; //metadata
    }
    
    /**
     * 提取组件元数据
     */
    DBOracleDGPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.ORACLE_CONST:
            data.INST_ID = element._id;
            break;
        case this.ORACLE_CONTAINER_CONST:
            data.INST_ID = element._id;
            data.DG_NAME = element.text;
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
    DBOracleDGPlate.prototype.getElementDeployed = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "1";
            element.removeEventListener('contextmenu');
            if (self.deployedMenu != null) {
                element.addEventListener('contextmenu', function(e) {
                    self.deployedMenu.show(e);
                });
            }
        } else if (element.elementType == "container") {
            if (element.parentContainer != undefined && element.parentContainer != null) {
                element.removeEventListener('contextmenu');
                if (self.deployedMenu != null) {
                    element.addEventListener('contextmenu', function(e) {
                        self.deployedMenu.show(e);
                    });
                }
            }
        }
    };
    
    /**
     * 缓存面板卸载
     */
    DBOracleDGPlate.prototype.undeployElement = function(element){
        if(element.parentContainer.childs.length <= 1 ){
            Component.Alert("error", "已经是最后一个组件！");
        } else {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    };
    
    /**
     * 组件卸载成功时的处理
     */
    DBOracleDGPlate.prototype.getElementUndeployed = function(element) {
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
