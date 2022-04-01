var Component = window.Component || {};


(function(Component) {
    /**
     *  面板基类
     */
    function Plate() {
        //常量
        this.fontColor = '3,13,247';
        this.font = '10pt 微软雅黑';
        this.borderColor = '170,170,170';
        this.fillColor = '245,245,245';
        this.borderWidth = 2;
        this.borderRadius = 10;
        this.defaultWidth = 30; //默认的node尺寸
        this.defaultHeight = 30;
        this.padding = 10;
        this.shortTimeout = 5000; //超时时间(短)
        this.longTimeout = 300000; //超时时间(长，如安装部署等)
        //这两个可以改成动态创建（类似popupForm），暂时写死
        this.$componentMetadata = $("#componentMetadata");
        this.$metadataModal = $('#metadata');
        //一些与html及外部有关的参数
        this.iconDir = "../images/console/"; //图标路径
        this.smsId ='';
        
        this.prometheusIcon = "prometheus-icon.png";
        this.grafanaIcon = "grafana-icon.png";
        
        this.DASHBOARD_PROXY_CONST = "DASHBOARD_PROXY";
        this.COLLECTD_CONST = "COLLECTD";
        this.PROMETHEUS_CONST = "PROMETHEUS";
        this.GRAFANA_CONST = "GRAFANA";
        this.ROCKETMQ_CONSOLE_CONST = "ROCKETMQ_CONSOLE";
        this.PULSAR_MANAGER_CONST = "PULSAR_MANAGER";

        //服务
        this.getTopoServ         = "paas/metadata/loadServiceTopo";         // 获取拓扑结构的服务（包括里面的组件）
        this.getUserServ         = "paas/metadata/getUserByServiceType";    // 获取服务器IP及操作系统用户信息
        this.getServList         = "paas/metadata/getServList";             // 获取指定类型的paas service列表信息
        this.saveTopoServ        = "paas/metadata/saveServiceTopoSkeleton"; // 保存拓扑结构的服务
        this.saveServiceNode     = "paas/metadata/saveServiceNode";         // 保存组件信息的服务
        this.delServiceNode      = "paas/metadata/delServiceNode";          // 删除组件信息的服务
        this.getServTypeVerList  = 'paas/metadata/getServTypeVerList'       // 服务版本
        
        this.deployService       = "paas/autodeploy/deployService";         // 部署服务
        this.undeployService     = "paas/autodeploy/undeployService";       // 卸载面板
        this.forceUndeployServ   = "paas/autodeploy/forceUndeployServ";     // 强制卸载组件

        this.deployInstance      = "paas/autodeploy/deployInstance";        // 部署实例
        this.undeployInstance    = "paas/autodeploy/undeployInstance";      // 卸载组件

        this.startInstance       = "paas/autodeploy/startInstance";         // 拉起实例
        this.stopInstance        = "paas/autodeploy/stopInstance";          // 停止实例
        this.restartInstance     = "paas/autodeploy/restartInstance";       // 重启实例
        this.updateInstance      = "paas/autodeploy/updateInstance";        // 更新实例
        this.batchUpdateInst     = "paas/autodeploy/batchUpdateInst";       // 批量更新实例
        
        this.checkInstanceStatus = "paas/autodeploy/checkInstanceStatus";   // 检查实例状态

        this.deployLog           = "paas/autodeploy/getDeployLog";          // 部署日志
        this.getAppLog           = "paas/autodeploy/getAppLog";             // 获取应用日志
        
        //状态图标
        this.deployedIcon = new Image();
        this.deployedIcon.src = this.iconDir + "status_deployed.png";
        
        this.savedIcon = new Image();
        this.savedIcon.src = this.iconDir + "status_saved.png";
        
        this.pitchIcon = new Image();
        this.pitchIcon.src = this.iconDir + "pitchIcon.png";
        
        this.errorIcon = new Image();
        this.errorIcon.src = this.iconDir + "status_error.png";
        
        this.warnIcon = new Image();
        this.warnIcon.src = this.iconDir + "status_warn.png";
        
        this.preEmbaddedIcon = new Image();
        this.preEmbaddedIcon.src = this.iconDir + "status_pre_embadded.png";
        
        this.setRootUrl = function (url) {
            this.url = url;
        }
        this.PlateType = null;
        this.plateMenu = null;
        this.deployedMenu = null;
        this.overridePlateMenu = false;
        this.showPlateMenu = true;
        this.showMetaDataOnMouse = true;
        
        this.statusInterval = "";

        //初始化舞台
        this.initStage = function (id, name, canvas, isProduct,version) {
            var self = this;
            this.id = id;
            this.name = name;
            this.isProduct = isProduct;
            this.version = version;

            this.canvas = canvas.getContext("2d");
            this.canvas.font = this.font;
            //默认的子container尺寸（通常包含两个node，一主一从）
            this.defaultContainerW = this.defaultWidth * 3 + this.padding;
            this.defaultContainerH = this.defaultHeight * 2 + this.canvas.measureText("田").width;
           
            this.stage = new JTopo.Stage(canvas);
            this.stage.clear();
            this.stage.wheelZoom = 0.85;
            this.scene = new JTopo.Scene();
            this.scene.childs=[];
            this.scene.clear();
            this.scene.mode = "normal";
            
            this.stage.add(this.scene);
            this.width = $(canvas).attr("width");
            this.height = $(canvas).attr("height");
            
            this.collectd = null;
            this.DashboardProxy = null;
            this.prometheus = null;
            this.grafana = null;
            this.PulsarManager = null;
            this.RocketMQConsole = null;
            
            if(!this.overridePlateMenu){
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
            }
            
            if (this.showPlateMenu) {
                this.scene.addEventListener('contextmenu', function(e) {
                    self.plateMenu.show(e);
                });
            }
            
            // 去除默认的右击菜单
            $(canvas).contextmenu(function(e) {
                e.preventDefault();
            });
        }
    }
    
    Component.Plate = Plate;
    
    Plate.prototype.loadSchema = function (schemaName) {
        if (!window[schemaName]) {
            var url = "./schema/" + schemaName;
            $.ajax({
                url: url,
                type: "get",
                async : false,
                success: function (data) {
                    window[schemaName] = data;
                }
            });
        }
    }

    //创建一个container容器
    Plate.prototype.makeContainer = function (x, y, text, rows, cols, type, eleType) {
        var container = null;
        if (type == "container") {
            container = new Component.FlexibleContainer(rows, cols, this.padding, this.canvas.measureText("田").width);
            container.height = (rows + 1) * this.defaultContainerH + rows * this.canvas.measureText("田").width + (rows - 1) * this.padding;
            container.width = (cols + 1) * this.defaultContainerW + (cols - 1) * this.padding;
        } else {
            container = new Component.FlexibleContainer(rows, cols, this.padding, this.canvas.measureText("田").width);
            container.height = (rows + 1) * this.defaultHeight + rows * this.canvas.measureText("田").width + (rows - 1) * this.padding;
            container.width = (cols + 1) * this.defaultWidth + (cols - 1) * this.padding;
        }
        if (eleType) {
            container.type = eleType;
        }
        container.fontColor = this.fontColor;
        container.font = this.font;
        container.borderColor = this.borderColor;
        container.fillColor = this.fillColor;
        container.borderWidth = this.borderWidth;
        container.borderRadius = this.borderRadius;

        container.x = x - container.width / 2;
        container.y = y - container.height / 2;
        container.dragable = true;
        container.text = text;
        container.textPosition = 'Top_Left';

        this.scene.add(container);
        return container;
    }

    Plate.prototype.makeNode = function (x, y, img, text, type, menu, status, isPitch, status, nodeType) {
        var node = new Component.StatusNode();
        node.font = this.font;
        node.fontColor = this.fontColor;
        node.setImage(img);
        node.text = text;
        node.dragable = true;
        node.x = x;
        node.y = y;
        node.width = this.defaultWidth;
        node.height = this.defaultHeight;
        node.type = type; //组件类型
        node.status = status;
        node.statusIcons = { "deployed": this.deployedIcon, 
                             "saved": this.savedIcon,
                             "error": this.errorIcon,
                             "warn": this.warnIcon,
                             "preEmbadded": this.preEmbaddedIcon }; //状态图标
        node.pitchStatus = isPitch == "master" ? (nodeType == 1 ? "master1":"master0" ): (nodeType == 0 ? "backup0":"backup1" );
        node.pitchIcon = this.pitchIcon; //状态图标
        node.addEventListener('contextmenu', function (e) {
            menu.show(e);
        });
        return node;
    }

    /**
     * 新增一个node到container中
     */
    Plate.prototype.addNodeToContainer = function (x, y, img, text, type, menu, container, needAlarm, isPitch, status, nodeType) {
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = "0";
        
        if (container != null && container.isInContainer(x, y)) {
            var posX = x - this.defaultWidth / 2;
            var posY = y - this.defaultHeight / 2;
            
            var node = this.makeNode(posX, posY, img, text, type, menu, status, isPitch, status, nodeType);
            this.scene.add(node);
            container.add(node);
            if (status == "-1") {
                this.popupForm(node); //弹出信息窗
            }
            return node;
        } else {
            if (needAlarm) {
                Component.Alert("warn", "请将组件拖放到对应的容器中！");
            }
            return null;
        }
    }

    //新增collectd
    Plate.prototype.addCollectd = function (x, y, img, text, type, menu, isSaved, status) {
        if (this.collectd != null) {
            Component.Alert("warn", "集群中只能有一个collectd！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.collectd = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }

    //新增dashboard_proxy
    Plate.prototype.addDashboardProxy = function (x, y, img, text, type, menu, isSaved, status) {
        if (this.DashboardProxy != null) {
            Component.Alert("warn", "集群中只能有一个dashboard_proxy！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.DashboardProxy = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }

    //新增prometheus
    Plate.prototype.addPrometheus = function (x, y, img, text, type, menu, isSaved, status) {
        if (this.prometheus != null) {
            Component.Alert("warn", "集群中只能有一个prometheus！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.prometheus = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }

    //新增grafana
    Plate.prototype.addGrafana = function (x, y, img, text, type, menu, isSaved, status) {
        if (this.grafana != null) {
            Component.Alert("warn", "集群中只能有一个grafana！");
            return false;
        }
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.height / 2 - this.scene.translateY - (this.height / 2 - y) / this.scene.scaleY;
        // var status = !isSaved ? "-1" : "0";
        var node = this.makeNode(x - this.defaultWidth / 2, y - this.defaultHeight / 2, img, text, type, menu, status);
        this.scene.add(node);
        this.grafana = node;
        if (!isSaved) {
            this.popupForm(node); //弹出信息窗
        }
        return true;
    }

    //新增一个container到container中
    Plate.prototype.addContainerToContainer = function (x, y, text, type, _rows, _cols, menu, container, isSaved) {
        x = this.width / 2 - this.scene.translateX - (this.width / 2 - x) / this.scene.scaleX;
        y = this.width / 2 - this.scene.translateY - (this.width / 2 - y) / this.scene.scaleY;

        if (container != null && container.isInContainer(x, y)) {
            var newContainer = this.makeContainer(x - this.defaultContainerW / 2, y - this.defaultContainerH / 2, text, 1, 2, "node");
            container.add(newContainer);
            newContainer.type = type;
            newContainer.addEventListener('contextmenu', function (e) {
                menu.show(e);
            });
            if (!isSaved) {
                newContainer.status = "-1";
                this.popupForm(newContainer); //弹出信息窗
            } else {
                newContainer.status = "saved";
            }

            return newContainer;
        } else {
            Component.Alert("warn", "请将组件拖放到对应的容器中！");
            return null;
        }
    }

    //从界面上删除选中的组件
    Plate.prototype.deleteComponent = function (element) {
        if (element.elementType == "container") {
            for (var i = element.childs.length - 1; i >= 0; i--) {
                var child = element.childs[i];
                element.remove(child);
                this.scene.remove(child);
            }
        }
        if (element.parentContainer) {
            element.parentContainer.remove(element);
        }
        this.scene.remove(element);
        if (element.type.indexOf(this.COLLECTD_CONST) != -1) {
            this.collectd = null;
        } else if (element.type.indexOf(this.PULSAR_MANAGER_CONST) != -1) {
            this.PulsarManager = null;
        } else if (element.type.indexOf(this.DASHBOARD_PROXY_CONST)) {
            this.DashboardProxy = null;
        } else if (element.type.indexOf(this.this.PROMETHEUS_CONST)) {
            this.prometheus = null;
        } else if (element.type.indexOf(this.this.GRAFANA_CONST)) {
            this.grafana = null;
        } else if (element.type.indexOf(this.this.ROCKETMQ_CONSOLE_CONST)) {
            this.RocketMQConsole = null;
        }
    }

    //后台删除选中的组件
    Plate.prototype.deleteComponentBackground = function (element) {
        var parentID = (element.parentContainer != undefined && element.parentContainer != null) ? element.parentContainer._id : this.id;
        var self = this;
        var reqData = {};
        reqData.PARENT_ID = parentID;
        reqData.INST_ID = element._id;
        // if(!self.deleteNodeCheck(self,element)){
        //     return;
        // }
        $.ajax({
            url: this.url + this.delServiceNode,
            async: true,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "删除组件信息成功！");
                    self.deleteComponent(element); //从jtopo图上删除组件
                } else {
                    Component.Alert("error", "删除组件信息失败！" + result.RET_INFO);
                }
            }
        });

    }

    // 删除节点时节点个数的判断
    Plate.prototype.deleteNodeCheck = function(self,element){
        if(self.PlateType == "SERVERLESS_APISIX"){
            if(self.isProduct == "1"){
                if(element.type=="ETCD"){
                    if(self.ApisixEtcdContainer.childs.length<=3){            
                        Component.Alert("warn", "目前环境为生产环境，etcd容器中至少存在3个组件!");
                        return false;
                    } 
                }else{
                    if(self.ApisixContainer.childs.length<=1){            
                        Component.Alert("warn", "apisix容器内容至少存在一个组件!");
                        return false;
                    } 
                }                      
            }else{
                if(element.type=="ETCD"){
                    if(self.ApisixEtcdContainer.childs.length<=1){            
                        Component.Alert("warn", "etcd容器内容至少存在一个组件!");
                        return false;
                    } 
                }else{
                    if(self.ApisixContainer.childs.length<=1){            
                        Component.Alert("warn", "apisix容器内容至少存在一个组件!");
                        return false;
                    } 
                }    
            }       
        }else if(self.PlateType == "MQ_ROCKETMQ"){
            var data = self.VbrokerContainer.childs;
            if(self.isProduct == "1"){
                if(element.type == 'ROCKETMQ_VBROKER'){
                    if(data.length<=1){    
                        Component.Alert("warn", "Vbroker容器内容至少存在一个组件!");
                        return false
                    }else{
                        if(element.childs.length>=1){
                            Component.Alert("warn", "容器中存在组件，请删除容器中组件在进行此操作!");
                            return false
                        }
                    }
                }else if(element.type == 'ROCKETMQ_NAMESRV'){
                    if(self.NamesrvContainer.childs.length<=2){            
                        Component.Alert("warn", "目前环境为生产环境，namesrv容器中至少存在2个组件!");
                        return false
                    }
                } else if(element.type == 'ROCKETMQ_BROKER'){
                    if(data.length==1 && data[0].childs[0]._id == element._id){
                        Component.Alert("warn", "broker容器内容至少存在一个组件!");
                        return false
                    }
                }  
            }else{
                if(element.type =='ROCKETMQ_VBROKER'){
                    if(data.length<=1){
                        Component.Alert("warn", "Vbroker容器内容至少存在一个组件!");
                        return false
                    }else{
                        if(element.childs.length>=1){
                            Component.Alert("warn", "容器中存在组件，请删除容器中组件在进行此操作!");
                            return false
                        }
                    }
                }else if(element.type == 'ROCKETMQ_NAMESRV'){
                    if(self.NamesrvContainer.childs.length<=1){
                        Component.Alert("warn", "namesrv容器中至少存在1个组件!");
                        return false
                    }
                } else if(element.type == 'ROCKETMQ_BROKER'){
                    if(data.length==1 && data[0].childs[0]._id == element._id){
                        Component.Alert("warn", "broker容器内容至少存在一个组件!");
                        return false
                    }
                }    
            }       
        }else if(self.PlateType == "DB_TDENGINE"){
            if(self.isProduct == "1"){
                if(element.type=="TD_DNODE"){
                    if(self.DnodeContainer.childs.length<=3){            
                        Component.Alert("warn", "目前环境为生产环境，dnode容器中至少存在3个组件!");
                        return false;
                    } 
                }else{
                    if(self.ArbitratorContainer.childs.length<=1){            
                        Component.Alert("warn", "arbitrator容器内有且只能有一个组件!");
                        return false;
                    } 
                }
            }else{
                if(element.type=="TD_ARBITRATOR"){
                    if(self.ArbitratorContainer.childs.length<=1){            
                        Component.Alert("warn", "arbitrator容器内有且只能有一个组件!");
                        return false;
                    } 
                }else{
                    if(self.DnodeContainer.childs.length<=1){            
                        Component.Alert("warn", "dnode容器内容至少存在一个组件!");
                        return false;
                    } 
                }    
            }   
        }else if(self.PlateType == "MQ_PULSAR"){
            if(self.isProduct == "1"){
                if(element.type=="PULSAR_BROKER"){
                    if(self.BrokerContainer.childs.length<=2){
                        Component.Alert("warn", "目前环境为生产环境，broker容器中至少存在2个组件!");
                        return false;
                    }
                }else if(element.type=="PULSAR_BOOKKEEPER"){
                    if(self.BookkeeperContainer.childs.length<=3){            
                        Component.Alert("warn", "bookkeeper容器内有且只能有3个组件!");
                        return false;
                    }
                }/*else if(element.type=="ZOOKEEPER"){
                    if(self.ZooKeeperContainer.childs.length<=1){            
                        Component.Alert("warn", "zookkeeper容器内有且只能有1个组件!");
                        return false;
                    }
                }*/
            }else{
                if(element.type=="PULSAR_BROKER"){
                    if(self.BrokerContainer.childs.length<=1){            
                        Component.Alert("warn", "broker容器中至少存在1个组件!");
                        return false;
                    }
                }else if(element.type=="PULSAR_BOOKKEEPER"){
                    if(self.BookkeeperContainer.childs.length<=1){            
                        Component.Alert("warn", "bookkeeper容器内有且只能有1个组件!");
                        return false;
                    }
                }/*else if(element.type=="ZOOKEEPER"){
                    if(self.ZooKeeperContainer.childs.length<=1){
                        Component.Alert("warn", "zookkeeper容器内有且只能有1个组件!");
                        return false;
                    } 
                }*/
            }   
        }else if(self.PlateType == "DB_ORACLE_DG"){   
            var data;
            if(element.parentContainer._id == self.MetadbContainer._id){
                data = self.MetadbContainer.childs;
            }else if(element.parentContainer._id == self.RealDB1Container._id){
                data = self.RealDB1Container.childs;
            }else if(element.parentContainer._id == self.RealDB2Container._id){
                data = self.RealDB2Container.childs;
            }else if(element.parentContainer._id == self.RealDB3Container._id){
                data = self.RealDB3Container.childs;
            }else if(element.parentContainer._id == self.RealDB4Container._id){
                data = self.RealDB4Container.childs;
            }
            if(data.length<=2){
                Component.Alert("warn", "容器内容至少存在2个组件!");
                return false
            }
        }

        return true;
    }

    //告警指定的节点
    Plate.prototype.alarmNode = function (name, info) {
        var nodes = this.scene.findElements(function (e) {
            return e.elementType == "node" && e.text == name;
        });
        $.each(nodes, function (_index, node) {
            node.alarm = info;
            node.alarmStyle = "flash";
        });
    }

    //获取集群拓扑数据
    Plate.prototype.getTopoData = function (id) {
        // var self = this;
        var value = null;
        var reqData = {};
        reqData.INST_ID = id;
        $.ajax({
            url: this.url + this.getTopoServ,
            async: false,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    value = result.RET_INFO;   
                } else if (result.RET_CODE == -5) {
                    value = "init";
                } else {
                    Component.Alert("error", "获取组件信息失败！" + result.RET_INFO);
                }
            }
        });
        return value; 
    }

    //设置组件是否已部署
    Plate.prototype.getDeployFlag = function (flag) {
        var self = this;
        var deployed = [];
        var warn = [];
        var error = [];
        var alarm = [];
        var preEmbadded = [];
        flag.forEach(function (obj) {
            for (var id in obj) {
                if (obj[id] == "1") {
                    deployed.push(id);
                } else if (obj[id] == "2") {
                    warn.push(id);
                } else if (obj[id] == "3") {
                    error.push(id);
                } else if (obj[id] == "4") {
                    alarm.push(id);
                } else if (obj[id] == "5") {
                    preEmbadded.push(id);
                }
            }
        });
        
        self.scene.childs.forEach(function (element) {
            deployed.forEach(function (id) {
                if (element._id == id) {
                    self.getElementDeployed(element);
                }
            });
        });
        
        self.scene.childs.forEach(function (element) {
            warn.forEach(function (id) {
                if (element._id == id) {
                    self.getElementWarn(element);
                }
            });
        });
        
        self.scene.childs.forEach(function (element) {
            error.forEach(function (id) {
                if (element._id == id) {
                    self.getElementError(element);
                }
            });
        });
        
        self.scene.childs.forEach(function (element) {
            alarm.forEach(function (id) {
                if (element._id == id) {
                    self.getElementAlarm(element);
                }
            });
        });
        
        self.scene.childs.forEach(function (element) {
            preEmbadded.forEach(function (id) {
                if (element._id == id) {
                    self.getElementEmbadded(element);
                }
            });
        });
    }

    //保存拓扑数据到后台
    Plate.prototype.saveTopoData = function (params, serverType) {
        var self = this,
            json = params ? plate.toPlateJson(false) : plate.toPlateJson(true),
            type = serverType ? serverType : this.PlateType;
        
        var reqData = {};
        reqData.TOPO_JSON = json;
        reqData.SERV_TYPE = type;
        $.ajax({
            url: this.url + this.saveTopoServ,
            async: true,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    self.needInitTopo = false;
                    //如果有传入参数，代表是从saveElement过来的，需要调用saveElement保存组件信息
                    if (params) {
                        self.saveElementData(params[0], params[1], params[2]);
                    } else {
                        Component.Alert("success", "保存面板信息成功！");   
                    }
                } else {
                    Component.Alert("error", "保存面板信息失败！" + result.RET_INFO);
                }
            }
        });
    }

    //保存组件（单个）数据到后台
    Plate.prototype.saveElementData = function (element, jsonString, popupForm) {
        //如果还没有保存拓扑结构，需要先保存拓扑结构
        if (this.needInitTopo) {
            this.saveTopoData([element, jsonString, popupForm]);
            popupForm.$submitBtn.removeAttr("disabled");
            return; //不需要再往下走了，由saveTopoData成功时调用
        }
        var parentID = (element.parentContainer != undefined && element.parentContainer != null) ? element.parentContainer._id : this.id;
        var data = {};
        var opType = element.status != "-1" ? 2 : 1;
        var json = JSON.parse(jsonString);
        //collectd需要保存位置信息
        if (element.type == this.COLLECTD_CONST || element.type == this.DASHBOARD_PROXY_CONST
                || element.type == this.PROMETHEUS_CONST || element.type == this.GRAFANA_CONST
                || element.type == this.ROCKETMQ_CONSOLE_CONST
                || element.type == this.PULSAR_MANAGER_CONST) {
            var pos = {};
            pos['x'] = element.x;
            pos['y'] = element.y;
            json['POS'] = pos;
        }

        data[element.type] = [];
        data[element.type].push(json); //单个组件，如果有需要也可以多个

        var self = this;
        var josnData = JSON.parse(jsonString);
        if (self.PlateType== "CACHE_REDIS_MASTER_SLAVE") {
            var redisData = self.NodeContainer.childs;
            if (self.checkisExistMaster(self,redisData,josnData.INST_ID) && josnData.NODE_TYPE == 1) {
                Component.Alert("warn", "容器中已经存在主组件！");
                popupForm.$submitBtn.removeAttr("disabled");
                return
            }
        } else if (self.PlateType == "DB_TDENGINE") {
            if (self.ArbitratorContainer.childs.length>1) {
                Component.Alert("warn", "容器中只能存放一个组件");
                popupForm.$submitBtn.removeAttr("disabled");
                return 
            }  
        } else if (self.PlateType=="MQ_ROCKETMQ") {
            if (element.type == "ROCKETMQ_BROKER") {
                var dataList = self.VbrokerContainer.childs;
                for (var i=0; i<dataList.length; i++) {
                    if (dataList[i]._id == parentID) {
                        if (dataList[i].childs.length>0) {
                            if (self.checkisExistMaster(self,dataList[i].childs,josnData.INST_ID) &&  josnData.BROKER_ROLE != 'SLAVE') {
                                Component.Alert("warn", "该容器中已经存在主组件！");
                                popupForm.$submitBtn.removeAttr("disabled");
                                return
                            }
                        }
                    }
                }
            }
        } else if(self.PlateType== "DB_ORACLE_DG") {
            if (element.elementType == "node") {
                var childs;
                if (element.parentContainer._id == self.MetadbContainer._id) {
                    childs = self.MetadbContainer.childs;
                } else if(element.parentContainer._id == self.RealDB1Container._id) {
                    childs = self.RealDB1Container.childs;
                } else if(element.parentContainer._id == self.RealDB2Container._id) {
                    childs = self.RealDB2Container.childs;
                } else if(element.parentContainer._id == self.RealDB3Container._id) {
                    childs = self.RealDB3Container.childs;
                } else if(element.parentContainer._id == self.RealDB4Container._id) {
                    childs = self.RealDB4Container.childs;
                }
                if (childs.length>2) {
                    Component.Alert("warn", "该容器中只能存放两个组件，一主一从");
                    popupForm.$submitBtn.removeAttr("disabled");
                    return 
                }  
                if (self.checkisExistMaster(self,childs,josnData.INST_ID) && josnData.NODE_TYPE == 1) {
                    Component.Alert("warn", "该容器中已经存在主组件！");
                    popupForm.$submitBtn.removeAttr("disabled");
                    return
                }
                
                if (self.checkisExistSubordinate(self,childs,josnData.INST_ID) && josnData.NODE_TYPE == 0) {
                    Component.Alert("warn", "该容器中已经存在从组件！");
                    popupForm.$submitBtn.removeAttr("disabled");
                    return
                }
            }
        }
       
        var reqData = {};
        reqData.NODE_JSON = data;
        reqData.PARENT_ID = parentID;
        reqData.OP_TYPE = opType;
        $.ajax({
            url: this.url + this.saveServiceNode,
            async: true,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {                
                    Component.Alert("success", "保存组件信息成功！");   
                    self.setMetaData(element, json);
                    element.status = "0";
                    Util.hideLoading();
                    popupForm.hide();
                    self.popElement = null;
                    popupForm.$submitBtn.removeAttr("disabled");

                    if(self.PlateType=="CACHE_REDIS_MASTER_SLAVE"){
                        var redisData = self.NodeContainer.childs;
                        if(!self.checkisExistMaster(self,redisData) &&  JSON.parse(jsonString).NODE_TYPE == 0){
                            Component.Alert("warn", "该容器中缺失主组件！");
                        }
                    }else if(self.PlateType=="MQ_ROCKETMQ"){
                        if(element.type == "ROCKETMQ_BROKER"){
                            var dataList = self.VbrokerContainer.childs;
                            for(var i=0; i<dataList.length; i++){
                                if(dataList[i]._id == parentID){
                                    if(dataList[i].childs.length>0){
                                        if(!self.checkisExistMaster(self,dataList[i].childs) &&  JSON.parse(jsonString).BROKER_ROLE == 'SLAVE'){
                                            Component.Alert("warn", "该容器中缺失主组件！");
                                        }
                                    }
                                }
                            }
                        }
                    }else if(self.PlateType=="CACHE_REDIS_HA_CLUSTER"){
                        element.borderColor ='90,179,69';
                    }else if(self.PlateType== "DB_ORACLE_DG"){
                        // 修改后数据后重新渲染数据
                        var data = self.getTopoData(self.id);
                        self.scene.clear();
                        self.initContainer(data);  
                    }
                } else {
                    Component.Alert("error", "保存组件信息失败！" + result.RET_INFO);
                    popupForm.$submitBtn.removeAttr("disabled");
                    Util.hideLoading();
                }
            }
        });
    }

    //检查单个容器中是否存在主组件
    Plate.prototype.checkisExistMaster = function(self,data,exceptInstId){
        if(data.length>0){
            if(self.PlateType=="CACHE_REDIS_MASTER_SLAVE" || self.PlateType== "DB_ORACLE_DG"){
                if(data[data.length-1].meta){
                    for(var i=0; i<data.length;i++){
                        if(data[i]._id == exceptInstId){
                            continue;
                        }
                        if(data[i].meta.NODE_TYPE == 1){
                            return true;
                        }
                    }
                }else{
                    for(var i=0; i<data.length-1;i++){
                        if(data[i].meta.NODE_TYPE == 1){
                            return true;
                        }
                    }
                }
            }else if(self.PlateType=="MQ_ROCKETMQ"){
                if(data[data.length-1].meta){
                    for(var i=0; i<data.length;i++){
                        if(data[i]._id == exceptInstId){
                            continue;
                        }
                        if(data[i].meta.BROKER_ROLE == "ASYNC_MASTER" || data[i].meta.BROKER_ROLE == "SYNC_MASTER"){
                            return true;
                        }
                    }
                }else{
                    for(var i=0; i<data.length-1;i++){
                        if(data[i].meta.BROKER_ROLE == "ASYNC_MASTER" || data[i].meta.BROKER_ROLE == "SYNC_MASTER"){
                            return true;
                        }
                    }
                }
            }   
           
        }
        return false;
    }

    //检查多个容器中是否存在主组件
    Plate.prototype.checkisAllExistMaster = function(self,data){
        var self = this;
        if(data.length>0){
            for(var i=0; i<data.length;i++){
                var isExist = self.checkisExistMaster(self,data[i].childs);
                if(!isExist){
                    return false;
                }
            }
        } 
        return true;
    }

    // 检查单个容器中是否存在从组件
    Plate.prototype.checkisExistSubordinate = function(self,data,exceptInstId){
        if(data.length>0){
            if(self.PlateType== "DB_ORACLE_DG"){
                if(data[data.length-1].meta){
                    for(var i=0; i<data.length;i++){
                        if(data[i]._id == exceptInstId){
                            continue;
                        }
                        if(data[i].meta.NODE_TYPE == 0){
                            return true;
                        }
                    }
                }else{
                    for(var i=0; i<data.length-1;i++){
                        if(data[i].meta.NODE_TYPE == 0){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // oracle_dg改变主从节点
    Plate.prototype.changeMasterSlave =function(container, data, smsId, oracleId, containerName){
        var self = this;
        var type ='';
        var arrData = data.ORACLE_DG_SERV_CONTAINER.DG_CONTAINER;
        for(var i=0; i<arrData.length; i++){
            if(container._id == arrData[i].INST_ID){
                if(arrData[i].ACTIVE_DB_TYPE == "backup"){
                    type = "master"
                }else{
                    type = "backup"
                }
            }
        } 
        var jsonData = {"SERV_INST_ID":smsId, "DB_SERV_INST_ID":oracleId, "ACTIVE_DB_TYPE":type, "DB_NAME":containerName}
        $.ajax({
            url: Url.serverList.switchSmsDBType,
            async: true,
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(jsonData),
            success: function (result) {
                if(result.RET_CODE == 0){
                    var data = self.getTopoData(oracleId);
                    self.scene.clear();
                    self.initContainer(data);
                    Component.Alert("success", "主从切换成功！");
                }else{
                    Component.Alert("warn", result.RET_INFO);
                }
               
            },
            error: function (_error) {
                Component.Alert("error", "主从切换失败！");
            }
        });
    }

    //部署组件（一个组件或一个面板）
    Plate.prototype.deployElement = function (element,deployFlag) {
        var key = this.randomStr(20, 'k');
        var self = this;
        var url = element ? this.url + this.deployInstance : this.url + this.deployService;
        var reqData = {};
        if (element) {
            reqData.INST_ID = element._id;
        }
        reqData.DEPLOY_FLAG = deployFlag;
        reqData.SERV_ID = this.id;
        reqData.LOG_KEY = key;
        $("#log").html("deploy start ...<br/>");
        if (this.PlateType=="CACHE_REDIS_MASTER_SLAVE") {
            var redisData = self.NodeContainer.childs;
            if(!self.checkisExistMaster(self,redisData)){
                Component.Alert("warn", "该容器中缺失主组件！");
                return 
            }
        } else if(this.PlateType=="MQ_ROCKETMQ") {
            var dataList = self.VbrokerContainer.childs;
            if (!self.checkisAllExistMaster(self,dataList)) {
                Component.Alert("warn", "vbroker容器中的部分容器中缺失主组件！");
                return
            }
        } else if(this.PlateType=="SMS_GATEWAY") {
            var SmsServerData = self.SmsServerContainer.childs;
            var SmsProcessData = self.SmsProcessContainer.childs;
            var SmsClientData = self.SmsClientContainer.childs;
            var SmsBatSaveData = self.SmsBatSaveContainer.childs;
            var SmsStatsData = self.SmsStatsContainer.childs;

            var data = [];
            data.push(SmsServerData, SmsProcessData, SmsClientData, SmsBatSaveData, SmsStatsData);
            
            for (var i=0; i<data.length; i++) {

                if(data[i].length < 1){
                    Component.Alert("warn", "每个容器至少需要存在一个组件！");
                    return
                }
            }
        } else if(this.PlateType=="SMS_QUERY_SERVICE") {
            var SmsQueryData = self.SmsQueryContainer.childs;
            
            if(SmsQueryData.length < 1){
                Component.Alert("warn", "sms_query容器至少需要存在一个组件！");
                return
            }
        } else if(this.PlateType=="DB_ORACLE_DG") {
            var metadbData = self.MetadbContainer.childs;
            var RealDB1Data = self.RealDB1Container.childs;
            var RealDB2Data = self.RealDB2Container.childs;
            var RealDB3Data = self.RealDB3Container.childs;
            var RealDB4Data = self.RealDB4Container.childs;

            var data = [];
            data.push(metadbData, RealDB1Data, RealDB2Data, RealDB3Data, RealDB4Data);
            
            for (var i=0; i<data.length; i++) {
                if(data[i].length != 2){
                    Component.Alert("warn", "部分容器中组件个数不足2个，要求一主一从！");
                    return
                }
            }
        } else if(this.PlateType=="DB_TIDB") {
            var PDData = self.PDContainer.childs;
            if((PDData.length % 2) == 0 ){
                Component.Alert("warn", "PD-Server容器组件个数不能为奇数个！");
                    return
            } 
        }
        
        if (!element) {
            if (!self.deployElementNodeCheck(self)) {
                return ;
            }
        }
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,
            complete : function () {
            },
            error: function (xhr) {
                clearInterval(myCurrInt);
                Component.Alert("error", "组件部署失败！" + xhr.status + ":" + xhr.statusText);
            },
            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "组件部署成功！");
                    if (element) {
                        self.getElementDeployed(element);
                    } else {
                        self.scene.childs.forEach(function (e) {
                            if (e.elementType != "link") {
                                self.getElementDeployed(e);
                            }
                        });
                    }
                } else {
                    Component.Alert("error", "组件部署失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
    }

    // 部署面板时对容器内节点的判断
    Plate.prototype.deployElementNodeCheck = function(self){
        if (self.PlateType == "SERVERLESS_APISIX") {
            if (self.isProduct == "1") {
                if (self.ApisixContainer.childs.length<1) {            
                    Component.Alert("warn", "容器内容至少存在一个组件!");
                    return false
                } 
                if (self.ApisixEtcdContainer.childs.length<3) {            
                    Component.Alert("warn", "目前环境为生产环境，etcd容器中至少存在3个组件!");
                    return false
                }                               
            } else {
                if (self.ApisixContainer.childs.length<1) {            
                    Component.Alert("warn", "容器内容至少存在一个组件!");
                    return false
                } 

                if(self.ApisixEtcdContainer.childs.length<1){            
                    Component.Alert("warn", "容器内容至少存在一个组件!");
                    return false
                }    
            }       
        } else if(self.PlateType == "MQ_ROCKETMQ") {
            var data = self.VbrokerContainer.childs;
            if (self.isProduct == "1") {
                if (data.length<1) {                            
                    Component.Alert("warn", "Vbroker容器内容至少存在一个组件!");
                    return false
                } else {
                    for (var i=0; i<data.length; i++) {
                        if (data[i].childs.length<1) {
                            Component.Alert("warn", "每个broker内容至少存在一个组件!");
                            return false;
                        }  
                    } 
                }
              
                if (self.NamesrvContainer.childs.length<2) {            
                    Component.Alert("warn", "目前环境为生产环境，namesrv容器中至少存在2个组件!");
                    return false
                }                               
            } else {
                if (data.length<1) {                            
                    Component.Alert("warn", "Vbroker容器内容至少存在一个组件!");
                    return false
                } else {
                    for (var i=0; i<data.length; i++) {
                        if (data[i].childs.length<1) {
                            Component.Alert("warn", "每个broker内容至少存在一个组件!");
                            return false;
                        }
                    } 
                }
              
                if (self.NamesrvContainer.childs.length<1) {            
                    Component.Alert("warn", "namesrv容器内容至少存在一个组件!");
                    return false
                }    
            }       
        } else if(self.PlateType == "DB_TDENGINE") {
            if (self.isProduct == "1") {
                if (self.DnodeContainer.childs.length<3) {            
                    Component.Alert("warn", "目前环境为生产环境，dnode容器中至少存在3个组件!");
                    return false;
                } 
                if (self.ArbitratorContainer.childs.length<1) {            
                    Component.Alert("warn", "arbitrator容器内有且只能有一个组件!");
                    return false;
                } 
            } else {
                if (self.ArbitratorContainer.childs.length<1) {            
                    Component.Alert("warn", "arbitrator容器内有且只能有一个组件!");
                    return false;
                } 
                if (self.DnodeContainer.childs.length<1) {            
                    Component.Alert("warn", "dnode容器内容至少存在一个组件!");
                    return false;
                } 
            }   
        } else if(self.PlateType == "MQ_PULSAR") {
            if (self.isProduct == "1") {
                if (self.BrokerContainer.childs.length<2) {
                    Component.Alert("warn", "目前环境为生产环境，broker容器中至少2个组件!");
                    return false;
                }
                if (self.BookkeeperContainer.childs.length<3) {
                    Component.Alert("warn", "bookkeeper容器内至少3个组件!");
                    return false;
                }
            } else {
                if (self.BrokerContainer.childs.length<1) {
                    Component.Alert("warn", "broker容器中至少有1个组件!");
                    return false;
                }
                if (self.BookkeeperContainer.childs.length<1) {
                    Component.Alert("warn", "bookkeeper容器内至少有1个组件!");
                    return false;
                }
            }
        }

        return true
    }

    Plate.prototype.unDeployPlate = function () {
        var key = this.randomStr(20, 'k'),
            self = this,
            url = this.url + this.undeployService;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.LOG_KEY = key;
        
        $("#log").html("undeploy start ...<br/>");

        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,
            complete : function () {
            },
            error: function (xhr) {
                clearInterval(myCurrInt);
                Component.Alert("error", "组件部署失败！" + xhr.status + ":" + xhr.statusText);
            },
            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "卸载成功！");
                    self.setAllUndeploy();
                } else {
                    Component.Alert("error", "卸载失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
    }

    Plate.prototype.forceUndeployPlate = function () {
        var key = this.randomStr(20, 'k'),
            self = this,
            url = this.url + this.forceUndeployServ;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.LOG_KEY = key;
        
        $("#log").html("undeploy start ...<br/>");

        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,
            complete : function () {
            },
            error: function (xhr) {
                clearInterval(myCurrInt);
                Component.Alert("error", "组件部署失败！" + xhr.status + ":" + xhr.statusText);
            },
            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "卸载成功！");
                    self.setAllUndeploy();
                } else {
                    Component.Alert("error", "卸载失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
    }

    // 卸载组件（只能一个组件）
    Plate.prototype.undeployElement = function (element) {
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.undeployInstance;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.LOG_KEY = key;
        
        $("#log").html("undeploy start ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "组件卸载成功！");
                    self.getElementUndeployed(element);
                } else {
                    Component.Alert("error", "组件卸载失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
    }

    Plate.prototype.openLayer = function (key) {
        var height = $(window).height() * 0.7;
        var width = $(window).width() * 0.7;
        var self = this;
        var myCurrInt = setInterval(function () {
            var logs = self.getDeployLog(key);
            if (self.isNotNull(logs)) {
                $("#log").append(logs);
                $("#log").parent().scrollTop($("#log").height() + 20);
            }
        }, 3000);

        layer.open({
            type: 1,
            title: ["控制台信息"],
            skin: 'layui-layer-self', //加上边框
            area: [width + 'px', height + 'px'], //宽高
            content: $("#log"),
            btn: ['停止刷新日志', '恢复刷新日志'],
            yes: function (_index, _layero) {
                clearInterval(myCurrInt);
            },
            btn2: function (_index, _layero) {
                clearInterval(myCurrInt);
                myCurrInt = setInterval(function () {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }, 3000);
                return false;
            },
            cancel: function () {
                clearInterval(myCurrInt);
            }
        });
        return myCurrInt;
    }

    Plate.prototype.openLayer2 = function () {
        var height = $(window).height() * 0.7;
        var width = $(window).width() * 0.7;

        layer.open({
            type: 1,
            title: ["控制台信息"],
            skin: 'layui-layer-self', //加上边框
            area: [width + 'px', height + 'px'], //宽高
            content: $("#log")
        });
    }
    
    Plate.prototype.waitAjax = async function () {
        return new Promise(resolve => setTimeout(resolve, 100));
    }
    
    //组件拉起定时检查
    Plate.prototype.setElementCkeckStatus = function(element) {
        if (element.elementType == "node") {
            clearInterval(this.statusInterval);
            
            element.status = "4";
            element.removeEventListener('contextmenu');
            this.statusInterval = setInterval(this.statusCheck(self._id, element.id, element.type), 3000);
        }
    };
    
    Plate.prototype.statusCheck = function(servId, instId, servType, _interval) {
        var that = this;
        
        var reqData = {};
        reqData.SERV_INST_ID = servId;
        reqData.INST_ID = instId;
        reqData.SERV_TYPE = servType;
        
        return function() {
            $.ajax({
                url : this.url + this.checkInstanceStatus,
                type: "post",
                dataType: "json",
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify(reqData),
                success: function (result) {
                    if(result.RET_CODE == 0){
                        //清除闪烁状态
                        clearInterval(that.statusInterval);
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
    
    // 启动组件
    Plate.prototype.startElement = function (element) {
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.startInstance;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.SERV_TYPE = element.type;
            reqData.LOG_KEY = key;
        
        $("#log").html("begin start ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "拉起成功！");
                    self.getElementStarted(element);
                } else {
                    Component.Alert("error", "拉起失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
        
        self.getElementWarn(element);
        // self.setElementCkeckStatus(element);
    }
    
    // 停止组件
    Plate.prototype.stopElement = function (element) {
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.stopInstance;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.SERV_TYPE = element.type;
            reqData.LOG_KEY = key;
        
        $("#log").html("begin stop ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "停止成功！");
                } else {
                    Component.Alert("error", "停止失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
        
        self.getElementWarn(element);
    }
    
    // 重启组件
    Plate.prototype.restartElement = function (element) {
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.restartInstance;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.SERV_TYPE = element.type;
            reqData.LOG_KEY = key;
        
        $("#log").html("begin restart ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "重启成功！");
                    self.getElementStarted(element);
                } else {
                    Component.Alert("error", "重启失败！" + result.RET_INFO);
                }
            }
        });
        var myCurrInt = this.openLayer(key);
        
        self.getElementWarn(element);
    }
    
    // 版本更新
    Plate.prototype.updateElement = function (element) {
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.updateInstance;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.SERV_TYPE = element.type;
            reqData.LOG_KEY = key;
        
        var myCurrInt = this.openLayer(key);
        self.getElementWarn(element);
        
        $("#log").html("begin update ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                
                if (result.RET_CODE == 0) {
                    // Component.Alert("success", "更新成功！");
                    self.getElementStarted(element);
                    
                    if (element.elementType == 'node'){
                        element.text = result.RET_INFO;  // 更新成功时通过RET_INFO返回给前端最后更新的版本
                    }
                } else {
                    Component.Alert("error", "更新失败！" + result.RET_INFO);
                }
            }
        });
    }

    function checkBatchUpdate(nodes) {
        var firstServType = "";
        $.each(nodes, function (_index, element) {
            var currServType = element.type;
            if (firstServType == "") {
                firstServType = currServType;
            } else {
                if (firstServType != currServType)
                    return false;
            }
            
        });
        
        return true;
    }

    //批量更新
    Plate.prototype.batchUpdateElement = function () {
        var nodes = this.scene.findElements(function (e) {
            return e.elementType == "node" && e.selected;
        });
        
        var key  = this.randomStr(20, 'k'),
            self = this,
            url  = this.url + this.batchUpdateInst,
            version = self.version;
        
        if (!checkBatchUpdate(nodes)) {
            Component.Alert("error", "单次批量更新只能选择同中类型实例 ...");
            return;
        }
        
        var inst_id_list = "";
        var serv_type = "";
        $.each(nodes, function (_index, element) {
            if (inst_id_list != "")
                inst_id_list += ",";
            
            inst_id_list += element._id;
            serv_type = element.type;
            self.getElementWarn(element);
        });
        
        var myCurrInt = this.openLayer(key);
        $("#log").append("开始批量更新<br/>");
        
        var reqData = {};
            reqData.SERV_ID = self.id,
            reqData.INST_ID_LIST = inst_id_list,
            reqData.SERV_TYPE = serv_type;
            reqData.LOG_KEY = key;
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,
            
            success: function (result) {
                clearInterval(myCurrInt);
                if (self.isNotNull(myCurrInt)) {
                    var logs = self.getDeployLog(key);
                    if (self.isNotNull(logs)) {
                        $("#log").append(logs);
                        $("#log").parent().scrollTop($("#log").height() + 20);
                    }
                }
                
                if (result.RET_CODE == 0) {
                    if (result.RET_INFO != null || result.RET_INFO != "") {
                        var str = result.RET_INFO;
                        var instIdList = str.split(",");
                        
                        $.each(instIdList, function (_index, id) {
                            var elems = self.scene.findElements(function (e) {
                                return e._id == id;
                            });
                            
                            if (elems != null && elems.length > 0) {
                                self.getElementStarted(elems[0]);
                                if (elems[0].elementType == 'node') {
                                    elems[0].text = version;
                                }
                            }
                        });
                    }
                    
                    Component.Alert("success", "批量更新成功！");
                }
            }
        });
    }
    
    Plate.prototype.getElementStarted = function(element) {
        var self = this;
        if (element.elementType == "node") {
            var embadded = element.meta.PRE_EMBEDDED;
            if (embadded != undefined && embadded == "true") {
                element.status = "5";
            } else {
                element.status = "1";
            }
            
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
    
    Plate.prototype.getElementWarn = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "2";
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
    
    Plate.prototype.getElementError = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "3";
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
    
    Plate.prototype.getElementAlarm = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "4";
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
    
    Plate.prototype.getElementEmbadded = function(element) {
        var self = this;
        if (element.elementType == "node") {
            element.status = "5";
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
    
    // 查询应用日志
    Plate.prototype.showAppLog = function (element, logType) {
        var self = this,
            url  = this.url + this.getAppLog;
        
        var reqData = {};
            reqData.SERV_ID = this.id,
            reqData.INST_ID = element._id,
            reqData.LOG_TYPE = logType;
        
        $("#log").html("日志获取中 ...<br/>");
        
        $.ajax({
            url: url,
            async: true,
            dataType: "json",
            data: JSON.stringify(reqData),
            contentType: "application/json; charset=utf-8",
            timeout: this.longTimeout,

            success: function (result) {
                var logs = result.RET_INFO;
                if (self.isNotNull(logs)) {
                    $("#log").append(logs);
                    $("#log").parent().scrollTop($("#log").height() + 20);
                }
            }
        });
        this.openLayer2();
    }

    Plate.prototype.getDeployLog = function (key) {
        var res,
            self = this;
        var reqData = {};
        reqData.LOG_KEY = key;
        if (this.isNotNull(key)) {
            $.ajax({
                url: this.url + this.deployLog,
                type: "post",
                async: false,
                dataType: "json",
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify(reqData),
                success: function (result) {
                    if (self.isNotNull(result))
                        res = result.RET_INFO;
                }
            });
        }
        return res;
    }

    //为表单获取服务器主机信息
    Plate.prototype.getSSHList = function (type, forms) {
        var reqData = {};
        reqData.SERV_CLAZZ = type;
        $.ajax({
            url: this.url + this.getUserServ,
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    forms.forEach(function (form) {
                        form.setUserInfo(result.RET_INFO);
                    });
                }
            }
        });
    }
    
    // sms网关 获取服务版本号
    Plate.prototype.getServVersion = function (type, forms) {
        var reqData = {};
        reqData.SERV_CLAZZ = type;
        $.ajax({
            url: this.url + this.getServTypeVerList,
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    forms.forEach(function (form) {
                        form.setservVersion(result.metaCmptVerMap);
                    });
                }
            }
        });
    }
    
    // getServList
    Plate.prototype.getServListByServType = function (type, forms) {
        var reqData = {};
        reqData.SERV_TYPE = type;
        $.ajax({
            url: this.url + this.getServList,
            type: "post",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(reqData),
            success: function (result) {
                if (result.RET_CODE == 0) {
                    forms.forEach(function (form) {
                        form.setUserInfo(result.RET_INFO, type);
                    });
                }
            }
        });
    }

    Plate.prototype.setAllUndeploy = function () {
        var self = this;
        this.scene.childs.forEach(function (element) {
            element.status = "0";
            if(element.childs){
                element.childs.forEach(function (child) {
                    child.status = "saved";
                    self.getElementUndeployed(child);
                })
            }
            self.getElementUndeployed(element);
        });
    }

    //随机串
    Plate.prototype.randomStr = function (len, radix) {
        radix = radix ? 10 : 36;
        var rdmString = "";
        for (; rdmString.length < len; rdmString += Math.random().toString(radix).substr(2));
        return rdmString.substr(0, len);
    }

    //是否为空
    Plate.prototype.isNotNull = function (s) {
        if (s != null && s != "" && s != undefined && s != "undefined" && s != "null") {
            return true;
        } else {
            return false;
        }
    }

    //展示组件元数据信息
    Plate.prototype.showMetadata = function (element, e) {
        if (!this.showMetaDataOnMouse) {
            return;
        }
        
        var self = this;
        self.$componentMetadata.html("");
        self.$componentMetadata.append('<tr class=""><td>ID</td> <td>'+element._id+'</td></tr>');
        // self.$componentMetadata.append('<tr class=""><td>NAME</td> <td>'+element.text+'</td></tr>');
        var meta = element.meta;

        // var windowInnerHeight = window.innerHeight;
        var windowInnerWidth  = window.innerWidth;
        // var x = e.x + 500 > windowInnerWidth ? e.offsetX : e.offsetX - 500;
        // var y = e.y - 500 > windowInnerHeight ? e.offsetY : e.offsetY - 500;

        for (var attr in meta) {
            switch(attr) {
                /*case "MASTER_ID":
                    element.childs.forEach(function (child) {
                        if (child._id == meta[attr]) {
                            self.$componentMetadata.append('<tr class=""><td>MASTER</td> <td>'+child.text+'</td></tr>');
                        }
                    });
                    break;
                case "OS_PWD":
                    break;
                default:
                    self.$componentMetadata.append('<tr class=""><td>'+attr+'</td> <td>'+meta[attr]+'</td></tr>');
                    break;*/
                case "IP":
                    self.$componentMetadata.append('<tr class=""><td>'+attr+'</td> <td>'+meta[attr]+'</td></tr>');
                    break;
                default:
                    break;
            }
        }
        self.$metadataModal.show();

        var eleWidth = self.$componentMetadata[0].offsetWidth;
        var eleHigh = self.$componentMetadata[0].offsetHeight;
        var x = 0;
        if (e.offsetX + eleWidth + 48 > windowInnerWidth) {
            x = windowInnerWidth - eleWidth - 5;
        } else {
            x = e.offsetX + 48;
        }
        var y = 0;
        if (e.offsetY <= eleHigh + 48) {
            y = 48;
        } else {
            y = e.offsetY - eleHigh - 24;
        }

        self.$metadataModal.css("left" , x);
        self.$metadataModal.css("top" , y);
    }

    Plate.prototype.hideMetadata = function (_element) {
        var self = this;
        self.$metadataModal.hide();
    }
})(Component);
