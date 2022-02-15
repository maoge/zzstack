var Component = window.Component || {};


(function(Component) {
    
    /**
     * Tidb面板类
     */
    function DBTidbPlate(url, id, name, canvas) {
        this.loadSchema("db.tidb.schema");
        
        Util.showLoading();
        this.setRootUrl(url);
        
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

        this.PlateType = "DB_TIDB";
        this.tikvStatusInterval = "";

        //图标(暂定)
        this.PDIcon = "db_pd_icon.png";
        this.TikvIcon = "db_tikv_icon.png";
        this.TidbIcon = "db_tidb_icon.png";
        this.DashboardProxyIcon = "db_dashboard_proxy.png";
        
        //常量
        this.PD_CONST = "PD_SERVER";
        this.TIKV_CONST = "TIKV_SERVER";
        this.TIDB_CONST = "TIDB_SERVER";
        
        this.showMetaDataOnMouse = false;
        
        this.PDContainer = null;
        this.TikvContainer = null;
        this.TidbContainer = null;
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
        this.PDForm = $.popupForm(this.PD_CONST, window['db.tidb.schema'], function(json){
            self.saveElementData(self.popElement, json, self.PDForm);
        }, cancelFunction);
        this.TikvForm = $.popupForm(this.TIKV_CONST, window['db.tidb.schema'], function(json){
            self.saveElementData(self.popElement, json, self.TikvForm);
        }, cancelFunction);
        this.TidbForm = $.popupForm(this.TIDB_CONST, window['db.tidb.schema'], function(json){
            self.saveElementData(self.popElement, json, self.TidbForm);
        }, cancelFunction);
        this.DashboardProxyForm = $.popupForm(this.DASHBOARD_PROXY_CONST, window['db.tidb.schema'], function(json){
            self.saveElementData(self.popElement, json, self.DashboardProxyForm);
        }, cancelFunction);
        
        this.getSSHList("DB", [this.PDForm, this.TikvForm, this.TidbForm, this.DashboardProxyForm]);

        //初始化Container
        this.initContainer(data);
    }

    DBTidbPlate.prototype = new Component.Plate();
    Component.DBTidbPlate = DBTidbPlate;


    /**
     * 初始化3个container：PD, Tikv, Tidb及各个容器的下属实例
     */
    DBTidbPlate.prototype.initContainer = function(data) {
        if (data != null) {
            var deployFlag = data.DEPLOY_FLAG;
            topoData = data.TIDB_SERV_CONTAINER;
            this.needInitTopo = false;

            var tidb_container = topoData.TIDB_SERVER_CONTAINER;
            var tikv_container = topoData.TIKV_SERVER_CONTAINER;
            var pd_container = topoData.PD_SERVER_CONTAINER;
            var dashboard_proxy = topoData.DASHBOARD_PROXY;

            this.PDContainer = this.makeContainer(pd_container.POS.x, pd_container.POS.y, "PD-Server集群", 
                    pd_container.POS.row, pd_container.POS.col, "container");
            this.PDContainer._id = pd_container.INST_ID;

            this.TikvContainer = this.makeContainer(tikv_container.POS.x, tikv_container.POS.y, "TiKV-Server集群",
                    tikv_container.POS.row, tikv_container.POS.col, "container");
            this.TikvContainer._id = tikv_container.INST_ID;
            
            this.TidbContainer = this.makeContainer(tidb_container.POS.x, tidb_container.POS.y, "TiDB-Server集群",
                    tidb_container.POS.row, tidb_container.POS.col, "container");
            this.TidbContainer._id = tidb_container.INST_ID;
                
            for (var i = 0; i < pd_container.PD_SERVER.length; i++) {
                var pd = pd_container.PD_SERVER[i];
                var node = this.addNodeToContainer(this.PDContainer.x+1, this.PDContainer.y+1,
                        this.iconDir+this.PDIcon, pd.INST_ID,
                        this.PD_CONST, this.nodeMenu, 
                        this.PDContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, pd);
            }
                
            for (var i = 0; i < tikv_container.TIKV_SERVER.length; i++) {
                var tikv = tikv_container.TIKV_SERVER[i];
                var node = this.addNodeToContainer(this.TikvContainer.x+1, this.TikvContainer.y+1, 
                        this.iconDir+this.TikvIcon, tikv.INST_ID,
                        this.TIKV_CONST, this.nodeMenu,
                        this.TikvContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, tikv);
            }
                
            for (var i = 0; i < tidb_container.TIDB_SERVER.length; i++) {
                var tidb = tidb_container.TIDB_SERVER[i];
                var node = this.addNodeToContainer(this.TidbContainer.x+1, this.TidbContainer.y+1, 
                        this.iconDir+this.TidbIcon, tidb.INST_ID,
                        this.TIDB_CONST, this.nodeMenu,
                        this.TidbContainer, true, false, STATUS_UNDEPLOYED);
                this.setMetaData(node, tidb);
            }
            
            if (dashboard_proxy && !$.isEmptyObject(dashboard_proxy)) {
                var x = dashboard_proxy.POS ? dashboard_proxy.POS.x : 0;
                var y = dashboard_proxy.POS ? dashboard_proxy.POS.y : 0;
                this.addDashboardProxy(x, y, this.iconDir+this.DashboardProxyIcon, 
                        dashboard_proxy.INST_ID, this.DASHBOARD_PROXY_CONST,
                        this.nodeMenu, true, STATUS_UNDEPLOYED);
                this.setMetaData(this.DashboardProxy, dashboard_proxy);
            }
            
            this.getDeployFlag(deployFlag);
        } else {
            this.needInitTopo = true;
            this.PDContainer = this.makeContainer(this.width*0.15, this.height*0.4, "PD-Server集群", 1, 3, "container");
            this.TikvContainer = this.makeContainer(this.width*0.55, this.height*0.6, "TiKV-Server集群", 1, 3, "container");
            this.TidbContainer = this.makeContainer(this.width*0.55, this.height*0.2, "TiDB-Server集群", 1, 3, "container");
        }
            
        //添加container连接
        var link = new JTopo.FlexionalLink(this.PDContainer, this.TikvContainer);
        link.direction = 'horizontal';
        this.scene.add(link);

        link = new JTopo.FlexionalLink(this.PDContainer, this.TidbContainer);
        link.direction = 'horizontal';
        this.scene.add(link);

        link = new JTopo.Link(this.TidbContainer, this.TikvContainer);
        link.direction = 'vertical';
        this.scene.add(link);

        Util.hideLoading();
    }

    //保存面板拓扑信息(位置信息等)
    DBTidbPlate.prototype.toPlateJson = function(_needCollectd) {
        var TIDB_SERV_CONTAINER = {};
        TIDB_SERV_CONTAINER.INST_ID = this.id;
        
        //TIDB集群
        var TIDB_SERVER_CONTAINER = {};
        TIDB_SERVER_CONTAINER.INST_ID = this.TidbContainer._id;
        TIDB_SERVER_CONTAINER.POS = this.TidbContainer.getPosJson();
        var TIDB_SERVER = [];
        TIDB_SERVER_CONTAINER.TIDB_SERVER = TIDB_SERVER;
        TIDB_SERV_CONTAINER.TIDB_SERVER_CONTAINER = TIDB_SERVER_CONTAINER;
        
        //TIKV集群
        var TIKV_SERVER_CONTAINER = {};
        TIKV_SERVER_CONTAINER.INST_ID = this.TikvContainer._id;
        TIKV_SERVER_CONTAINER.POS = this.TikvContainer.getPosJson();
        var TIKV_SERVER = [];
        TIKV_SERVER_CONTAINER.TIKV_SERVER = TIKV_SERVER;
        TIDB_SERV_CONTAINER.TIKV_SERVER_CONTAINER = TIKV_SERVER_CONTAINER;
        
        //PD集群
        var PD_SERVER_CONTAINER = {};
        PD_SERVER_CONTAINER.INST_ID = this.PDContainer._id;
        PD_SERVER_CONTAINER.POS = this.PDContainer.getPosJson();
        var PD_SERVER = [];
        PD_SERVER_CONTAINER.PD_SERVER = PD_SERVER;
        TIDB_SERV_CONTAINER.PD_SERVER_CONTAINER = PD_SERVER_CONTAINER;
        
        var proxy = {};
        if (this.DashboardProxy != null) {
            proxy.INST_ID = this.DashboardProxy._id;
            proxy.SSH_ID = this.DashboardProxy.SSH_ID;
            proxy.DASHBOARD_PORT = this.DashboardProxy.DASHBOARD_PORT;
            proxy.DASHBOARD_PD_ID = this.DashboardProxy.DASHBOARD_PD_ID;
            var pos = {};
            pos.x = this.DashboardProxy.x+this.DashboardProxy.width/2;
            pos.y = this.DashboardProxy.y+this.DashboardProxy.height/2;
            proxy.POS = pos;
        }
        TIDB_SERV_CONTAINER.DASHBOARD_PROXY = proxy;
        
        return {"TIDB_SERV_CONTAINER": TIDB_SERV_CONTAINER};
    }
        
    // Tidb面板新增组件
    DBTidbPlate.prototype.newComponent = function(x, y, datatype) {
        var container, img, text;
        switch(datatype) {
        case this.PD_CONST:
            container = this.PDContainer;
            img = this.iconDir+this.PDIcon;
            text = "PD";
            break;
        case this.TIKV_CONST:
            container = this.TikvContainer;
            img = this.iconDir+this.TikvIcon;
            text = "TIKV";
            break;
        case this.TIDB_CONST:
            container = this.TidbContainer;
            img = this.iconDir+this.TidbIcon;
            text = "TIDB";
            break;
        case this.DASHBOARD_PROXY_CONST:
            return this.addDashboardProxy(x, y, this.iconDir+this.DashboardProxyIcon, "dashboard_proxy", datatype, this.nodeMenu, false, STATUS_NEW);
        }
        return this.addNodeToContainer(x, y, img, text, datatype, this.nodeMenu, container, false, true, STATUS_NEW) != null;
    }

    //弹出窗口
    DBTidbPlate.prototype.popupForm = function(element,type) {
        this.popElement = element; //存放目前正在填写信息的元素，表单窗口关闭时置为null
        switch(element.type) {
        case this.PD_CONST:
            this.PDForm.show(this.getMetaData(element),type);
            break;
        case this.TIKV_CONST:
            this.TikvForm.show(this.getMetaData(element),type);
            break;
        case this.TIDB_CONST:
            this.TidbForm.show(this.getMetaData(element),type);
            break;
        case this.DASHBOARD_PROXY_CONST:
            this.DashboardProxyForm.show(this.getMetaData(element),type);
            break;
        }
    }
        
    //Tidb面板设置组件元数据
    DBTidbPlate.prototype.setMetaData = function(element, data) {
        var that = this;
        var id = data.INST_ID;

        switch(element.type) {
        case this.PD_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.TIKV_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.TIDB_CONST:
            //id = data.INST_ID;
            delete data.INST_ID;
            element.addEventListener('mouseover', function(e) {
                that.showMetadata(e.target, e);
            });
            element.addEventListener('mouseout', function(e) {
                that.hideMetadata(e.target);
            });
            break;
        case this.DASHBOARD_PROXY_CONST:
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
        
    //提取组件元数据
    DBTidbPlate.prototype.getMetaData = function(element) {
        var data = {};
        switch(element.type) {
        case this.PD_CONST:
            data.INST_ID = element._id;
            break;
        case this.TIKV_CONST:
            data.INST_ID = element._id;
            break;
        case this.TIDB_CONST:
            data.INST_ID = element._id;
            break;
        case this.DASHBOARD_PROXY_CONST:
            data.INST_ID = element._id;
            var pos = {};
            pos.x = this.DashboardProxy.x;
            pos.y = this.DashboardProxy.y;
            data.POS = pos;
            break;
        }
        for (var i in element.meta) {
            data[i] = element.meta[i];
        }
        return data;
    }

    //组件部署成功时的处理
    DBTidbPlate.prototype.getElementDeployed = function(element) {
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

    //tidb卸载元素
    DBTidbPlate.prototype.undeployElement = function(element){
        var res = true;
        switch (element.type) {
        case this.TIDB_CONST:
            if(element.parentContainer.childs.length <= 1 ){
                Component.Alert("error", "TIDB的数量不能小于1！");
                res = false;
            }
            break;
        case this.TIKV_CONST:
            if(element.parentContainer.childs.length <= 3 ){
                Component.Alert("error", "TIKV的数量不能小于3！");
                res = false;
            }
            break;
        case this.PD_CONST:
            if(element.parentContainer.childs.length <= 3 ){
                Component.Alert("error", "PD的数量不能小于3！");
                res = false;
            }
            break;
        case this.DASHBOARD_PROXY_CONST:
            break;
        }

        if (res) {
            Component.Plate.prototype.undeployElement.call(this, element);
        }
    }

    //组件卸载成功时的处理
    DBTidbPlate.prototype.getElementUndeployed = function(element) {
        if (element.elementType == "node") {
            //如果是tikv的卸载，特殊处理
            if(element.type == "TIKV_SERVER"){
                element.status = "4";
                var self = this;
                element.removeEventListener('contextmenu');
                //console.log(element);
                this.tikvStatusInterval = setInterval(this.tikvStatusCheck(id, element.meta.IP+":"+element.meta.PORT, element),1000);
                return;
            }
            element.status = "0";
            var self = this;
            element.removeEventListener('contextmenu');
            element.addEventListener('contextmenu', function(e) {
                self.nodeMenu.show(e);
            });
        }
    }

    DBTidbPlate.prototype.tikvStatusCheck = function(servId, instAddr, element, _interval) {
        var that = this;
        return function() {
            $.ajax({
                "url" : url + 'tidbsvr/tikvStatusService',
                "data" : {"SERV_ID":servId, "INST_ADD":instAddr},
                complete : function () {
                },
                success: function (data) {
                    var store = data.RET_INFO;
                    if(store == null || store == ""){
                        //清除闪烁状态
                        clearInterval(that.tikvStatusInterval);
                        element.status = "0";
                        element.removeEventListener('contextmenu');
                        element.addEventListener('contextmenu', function(e) {
                            self.nodeMenu.show(e);
                        });
                    }
                }
            });
        };
    }

})(Component);
