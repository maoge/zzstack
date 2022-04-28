var Component = window.Component || {};


(function(Component) {
    
    /**
     * Store-Minio面板类
     */
    function StoreMinioPlate(url, id, name, canvas, isProduct) {
        this.loadSchema("store.minio.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "STORE_MINIO";
        
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
        this.MinioIcon = "minio_128.png";
        
        //常量
        this.MINIO_CONTAINER_CONST = "MINIO_CONTAINER";
        this.MINIO_CONST = "MINIO";
        
        this.showMetaDataOnMouse = false;
        
        this.MinioContainer = null;
        this.Minio = null;
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
                        var data = self.MinioContainer.childs;
                        if(data.length==1){
                            Component.Alert("warn","minio容器中至少存在一个节点！");
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
        
        this.MinioForm = $.popupForm(this.MINIO_CONST, window['store.minio.schema'], function(json) {
            self.saveElementData(self.popElement, json, self.MinioForm);
        }, cancelFunction);
        
        this.getSSHList("KVSTORE", [this.MinioForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    StoreMinioPlate.prototype = new Component.Plate();
    Component.StoreMinioPlate = StoreMinioPlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    StoreMinioPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.MINIO_SERV_CONTAINER;
            this.needInitTopo = false;

            var MINIO_CONTAINER = topoData.MINIO_CONTAINER;

            this.MinioContainer = this.makeContainer(
                MINIO_CONTAINER.POS.x, MINIO_CONTAINER.POS.y,
                'minio容器',
                MINIO_CONTAINER.POS.row, MINIO_CONTAINER.POS.col, "container");
            this.MinioContainer._id = MINIO_CONTAINER.INST_ID;

            for (var i=0; i<MINIO_CONTAINER.MINIO.length; i++) {
                var minioNode = MINIO_CONTAINER.MINIO[i];
                var node = this.addNodeToContainer(this.MinioContainer.x+1,
                    this.MinioContainer.y+1, this.iconDir+this.MinioIcon,
                    minioNode.INST_ID, this.MINIO_CONST, this.nodeMenu,
                    this.MinioContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, minioNode);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.MinioContainer = this.makeContainer(this.width*0.4, this.height*0.2, "minio容器", 1, 2, "container");
        }

        //添加container连接

        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    StoreMinioPlate.prototype.toPlateJson = function() {
        var MINIO_SERV_CONTAINER = {};
        MINIO_SERV_CONTAINER.INST_ID = this.id;
        
        //redis集群
        var MINIO_CONTAINER = {};
        MINIO_CONTAINER.INST_ID = this.MinioContainer._id;
        MINIO_CONTAINER.POS = this.MinioContainer.getPosJson();
        var MINIO = [];
        MINIO_CONTAINER.MINIO = MINIO;
        MINIO_SERV_CONTAINER.MINIO_CONTAINER = MINIO_CONTAINER;
        
        //当第一次保存面板或collectd为空时，不需要传collectd信息
        /*var collectd = {};
        if (needCollectd && this.collectd != null) {
            collectd.INST_ID = this.collectd._id;
            var pos = {};
            pos.x = this.collectd.x+this.collectd.width/2;
            pos.y = this.collectd.y+this.collectd.height/2;
            collectd.POS = pos;
        }
        REDIS_SERV_CLUSTER_CONTAINER.COLLECTD = collectd;*/
        
        return {"MINIO_SERV_CONTAINER": MINIO_SERV_CONTAINER};
    };
    
    /**
     * 缓存面板新增组件
     */
    StoreMinioPlate.prototype.newComponent = function(x, y, datatype) {
        switch(datatype) {
        case this.MINIO_CONST:
            return this.addNodeToContainer(x, y, this.iconDir+this.MinioIcon, 
                "minio", datatype, this.nodeMenu, this.MinioContainer, false, true, STATUS_NEW) != null;
        }
    };
    
    /**
     * 弹出窗口
     */
    StoreMinioPlate.prototype.popupForm = function(element,type) {
        this.popElement = element;
        switch(element.type) {
        case this.MINIO_CONST:
            this.MinioForm.show(this.getMetaData(element), type);
            break;
        }
    };
    
    /**
     * 面板设置组件元数据
     */
    StoreMinioPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;
        switch(element.type) {
        case this.MINIO_CONST:
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
    StoreMinioPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.MINIO_CONST:
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
    StoreMinioPlate.prototype.getElementDeployed = function(element) {
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
    StoreMinioPlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.MINIO_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "minio节点的数量不能小于1！");
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
    StoreMinioPlate.prototype.getElementUndeployed = function(element) {
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
