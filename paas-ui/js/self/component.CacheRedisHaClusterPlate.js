var Component = window.Component || {};


(function(Component) {
    
    /**
     * redis ha cluster面板类
     */
    function CacheRedisHaClusterPlate(url, id, name, canvas) {
        this.loadSchema("cache.redis.ha.cluster.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        this.PlateType = "CACHE_REDIS_HA_CLUSTER";
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
        
        //常量
        this.HA_CONTAINER_CONST = "HA_CONTAINER";
        
        this.showMetaDataOnMouse = false;
        
        this.RedisClusterAContainer = null;
        this.RedisClusterBContainer = null;

        Util.hideLoading();
        var self = this;
        //初始化右键菜单
        this.containerMenu = $.contextMenu({
            items:[
                   {label:'修改信息', icon:'../images/console/icon_edit.png', callback: function(e){   
                       self.popupForm(e.target,"edit");
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
                        self.deployElement(e.target, "2");
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
        
        //初始化弹出表单
        var cancelFunction = function(){
            if (self.popElement.status=="-1") self.deleteComponent(self.popElement);
            self.popElement = null;
        };
        
        this.HaContainerForm = $.popupForm(this.HA_CONTAINER_CONST, window['cache.redis.ha.cluster.schema'],function(json){
            self.saveElementData(self.popElement, json, self.HaContainerForm);
        }, cancelFunction);

        this.getServListByServType("CACHE_REDIS_CLUSTER", [this.HaContainerForm]);
        
        //初始化Container
        this.initContainer(data);
    }
    
    CacheRedisHaClusterPlate.prototype = new Component.Plate();
    Component.CacheRedisHaClusterPlate = CacheRedisHaClusterPlate;
    
    /**
     * 初始化接入机、redis集群container
     */
    CacheRedisHaClusterPlate.prototype.initContainer = function(data) {
        var self = this
        if (data != null) {
            // TODO
            topoData = data.REDIS_HA_CLUSTER_CONTAINER;
            this.needInitTopo = false;
            
            var HA_CONTAINER = topoData.HA_CONTAINER; 
            for (var i=0; i<HA_CONTAINER.length; i++) {
                
                var item = HA_CONTAINER[i];
                var haName = item.SERV_CONTAINER_NAME;

                var pos = item.POS;
                var container = this.makeContainer(pos.x, pos.y, haName, pos.row, pos.col, "container");
                container._id = item.INST_ID;
                if ( haName == "RedisClusterA" ) {
                    this.RedisClusterAContainer = container;
                    var isExiseA = item.REDIS_SERV_CLUSTER_CONTAINER
                    if( Object.keys(isExiseA).length !=0){
                        this.RedisClusterAContainer.borderColor ='90,179,69';
                    }
                } else if( haName == "RedisClusterB" ){
                    this.RedisClusterBContainer = container;
                    var isExiseB = item.REDIS_SERV_CLUSTER_CONTAINER
                    if(Object.keys(isExiseB).length !=0){
                        this.RedisClusterBContainer.borderColor ='90,179,69';
                    }
                } 
                this.setMetaData(container, item);
            }

        } else {
            this.needInitTopo = true;
            
            this.RedisClusterAContainer = this.makeContainer(this.width*0.3, this.height*0.4, "RedisClusterA", 1, 1, "container");
            this.RedisClusterBContainer = this.makeContainer(this.width*0.5, this.height*0.4, "RedisClusterB", 1, 1, "container");
        }
        this.RedisClusterAContainer.addEventListener('contextmenu', function(e) {
            self.containerMenu.show(e);
        });
        this.RedisClusterBContainer.addEventListener('contextmenu', function(e) {
            self.containerMenu.show(e);
        });
        Util.hideLoading();
    }
    
    /**
     * 保存面板拓扑信息(位置信息等)
     */
    CacheRedisHaClusterPlate.prototype.toPlateJson = function(_needCollectd) {
        var REDIS_HA_CLUSTER_CONTAINER = {};
        REDIS_HA_CLUSTER_CONTAINER.INST_ID = this.id;
      
        var HA_CONTAINER = new Array(2);
        
        var CONTAINERA = {};
        CONTAINERA.INST_ID = this.RedisClusterAContainer._id;
        CONTAINERA.SERV_CONTAINER_NAME = this.RedisClusterAContainer.text;
        CONTAINERA.POS = this.RedisClusterAContainer.getPosJson();
        
        var CONTAINERB = {};
        CONTAINERB.INST_ID = this.RedisClusterBContainer._id;
        CONTAINERB.SERV_CONTAINER_NAME = this.RedisClusterBContainer.text;
        CONTAINERB.POS = this.RedisClusterBContainer.getPosJson();
        
        HA_CONTAINER[0] = CONTAINERA;
        HA_CONTAINER[1] = CONTAINERB;

        REDIS_HA_CLUSTER_CONTAINER.HA_CONTAINER = HA_CONTAINER;
        return {"REDIS_HA_CLUSTER_CONTAINER": REDIS_HA_CLUSTER_CONTAINER};
    };
    
    /**
     * 弹出窗口
     */
    CacheRedisHaClusterPlate.prototype.popupForm = function(element,type) {
        element.type = this.HA_CONTAINER_CONST;
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        this.HaContainerForm.show(this.getMetaData(element),type);
    };
    
    /**
     * redis面板设置组件元数据
     */
    CacheRedisHaClusterPlate.prototype.setMetaData = function(element, data) {
        var id = data.INST_ID;
        var servContainerName = data.SERV_CONTAINER_NAME
        delete data.INST_ID;
        element._id = id;
        element.text = servContainerName
        element.meta = data; //metadata
    }
    
    /**
     * 提取组件元数据
     */
    CacheRedisHaClusterPlate.prototype.getMetaData = function(element) {
        var data = {};
        data.INST_ID = element._id;
        data.SERV_CONTAINER_NAME = element.text;
        for (var i in element.meta) {
            data[i] = element.meta[i];
        }
        return data;
    }
       /**
     * 组件部署成功时的处理
     */
    CacheRedisHaClusterPlate.prototype.getElementDeployed = function(element) {
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
     * 组件卸载成功时的处理
     */
        CacheRedisHaClusterPlate.prototype.getElementUndeployed = function(element) {
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
