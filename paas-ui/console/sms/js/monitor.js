
(function ($,window,JTopo) {

    var SmsMonitor = function (options) {
        this.$canvas = options.canvas;
        this.basicUrl = options.basicUrl;
        //常量
        this.fontColor = '3,13,247';
        this.font = '10pt 微软雅黑';
        this.borderColor = '170,170,170';
        this.fillColor = '245,245,245';
        this.borderWidth = 2;
        this.borderRadius = 10;
        this.padding = 15;
        this.spqInLineArr     = [];
        this.spqOutLineArr    = [];
        this.spqOutLineNodeArr= [];
        this.chanTaskNodeArr  = [];
        this.chanTaskOutNodeArr  = [];
        this.saveRptInLineNodeArr      = [];
        this.saveMtInLineNodeArr       = [];
        this.saveRptOutLineNodeArr     = [];
        this.saveMtOutLineNodeArr      = [];
        this.saveRepOutNodeArr   = [];
        this.mqReadyLimit = options.mqReadyLimit;
        this.redisQueueLimit = options.redisQueueLimit;
        this.floatDivTable = options.floatDivTable;
        this.floatDiv = options.floatDiv;
        //初始化
        this.init();
        this.autoRefresh();
    };

    SmsMonitor.prototype.init = function() {
        //初始化舞台
        this.initState();
    }

    SmsMonitor.prototype.autoRefresh = function () {
        var that = this;
        this.currInterval = setInterval(function () {
            that.getAjaxData(true);
        }, 10000);
    }

    SmsMonitor.prototype.refresh = function (data) {
        let spqQueue = data["spq:queue:"],
            chanTask = data["chantask:queue:"],
            saveRep  = data["Q:saveRptTask:"],
            saveMt   = data["Q:saveMtTask:"];

        for(let i = 0; i< spqQueue.length; i++) {
            this.spqInLineArr[i].text  = "in:" +spqQueue[i]["produceRate"];
            this.spqOutLineArr[i].text = "out:"+spqQueue[i]["consumeRate"];
            this.spqOutLineNodeArr[i].text = "剩余:" + spqQueue[i]["msgReady"];
            if(spqQueue[i]["msgReady"] >= this.mqReadyLimit) {
                this.spqOutLineNodeArr[i].fillColor = "255,0,0";
                this.spqOutLineNodeArr[i].fontColor = "255,0,0";
            } else {
                this.spqOutLineNodeArr[i].fillColor = "0,111,255";
                this.spqOutLineNodeArr[i].fontColor = "0,0,255";
            }
        }


        for(let i = 0; i< chanTask.length; i++) {
            this.chanTaskNodeArr[i].text = chanTask[i]["queueName"];
            this.chanTaskOutNodeArr[i].text = "剩余:" + chanTask[i]["count"];
            if(chanTask[i]["count"] >= this.redisQueueLimit) {
                this.chanTaskOutNodeArr[i].fillColor = "255,0,0";
                this.chanTaskOutNodeArr[i].fontColor = "255,0,0";
            }else {
                this.chanTaskOutNodeArr[i].fillColor = "0,111,255";
                this.chanTaskOutNodeArr[i].fontColor = "0,0,255";
            }
            this.chanTaskNodeArr[i].meta = chanTask[i];
        }

        for(let i = 0; i< saveRep.length; i++) {
            this.saveRepOutNodeArr[i].text = "saveRpt剩余:"+saveRep[i]["msgReady"]+"\nsaveMt 剩余:"+ saveMt[i]["msgReady"];
            this.saveRptInLineNodeArr[i].text = "in:"+saveRep[i]["produceRate"];
            this.saveMtInLineNodeArr[i].text = "in:"+saveMt[i]["produceRate"];
            this.saveRptOutLineNodeArr[i].text = "out:"+saveRep[i]["consumeRate"];
            this.saveMtOutLineNodeArr[i].text = "out:"+saveMt[i]["consumeRate"];

            if(saveRep[i]["msgReady"] >= this.mqReadyLimit || saveMt[i]["msgReady"] >= this.mqReadyLimit) {
                this.saveRepOutNodeArr[i].fillColor = "255,0,0";
                this.saveRepOutNodeArr[i].fontColor = "255,0,0";
            } else {
                this.saveRepOutNodeArr[i].fillColor = "0,111,255";
                this.saveRepOutNodeArr[i].fontColor = "0,0,255";
            }
        }
    }

    SmsMonitor.prototype.initState = function() {
        var canvas = this.$canvas[0];

        //设置canvas的div元素的高度和宽度，不然会失真
        this.width = canvas.offsetWidth - 2;
        this.heigth = canvas.offsetHeight - 22;
        this.$canvas.attr("width", this.width);
        this.$canvas.attr("height", this.heigth);

        var stage = new JTopo.Stage(canvas);
        //启动滚轮缩放
        stage.wheelZoom = 0.85;

        var scene = this.scene = new JTopo.Scene(stage);
        scene.alpha = 1;
        scene.mode = "normal";

        stage.add(scene);
        this.getAjaxData();
    }

    SmsMonitor.prototype.showDetail = function(element, e) {
        let self = this;

        this.floatDivTable.html("");
        let detail = element.meta.detail;
        let i = 0;
        for (let index in detail) {
            if(index == "countVal"){
                continue;
            }
            this.floatDivTable.append("<tr><td>" +element.meta.queueName +":"+ i++ +"</td><td>" + detail[index] + "</td></tr>");
        }

        this.floatDiv.show();
        var windowInnerWidth  = window.innerWidth;

        var eleWidth = self.floatDiv[0].offsetWidth;
        var eleHigh = self.floatDiv[0].offsetHeight;
        var x = 0;
        if(e.offsetX + eleWidth + 30 > windowInnerWidth){
            x = windowInnerWidth - eleWidth - 5;
        }else{
            x = e.offsetX + 48;
        }
        var y = 0;
        if(e.offsetY <= eleHigh){
            y = 48;
        }else {
            y = e.offsetY;
        }

        self.floatDiv.css("left" , x);
        self.floatDiv.css("top" , y);

    }

    SmsMonitor.prototype.hideDetail = function(element, e) {
        var that = this;
        this.floatDiv.hide();
    }

    SmsMonitor.prototype.getAjaxData = function(isRefresh) {
        var that = this;
        let req = {
            url : that.basicUrl,
            type : "post",
            success : function (data) {
                if(!isRefresh){
                    that.initData(data);
                    return;
                }
                that.refresh(data);
                Util.saveInterval(that.currInterval);
            },
            beforeSend : function(){},
            error : function () {
                Util.alert("error", "服务不可用！");
                if(!isRefresh) {
                    clearInterval(that.currInterval);
                }
            },
            complete : function () {
                Util.hideLoading();
            }
        };
        $.ajax(req);
    }

    SmsMonitor.prototype.initData =function(data) {
        let spqQueue = data["spq:queue:"],
            chanTask = data["chantask:queue:"],
            saveRep  = data["Q:saveRptTask:"],
            saveMt   = data["Q:saveMtTask:"],

            that     = this,

            startX = 100,
            startY = this.heigth / 2 - 23;

        let serverNode1 =  this.makeDashedNode('server组', startX, startY, 'sms/img/server.png');
        let processNode = this.makeDashedNode("process组" , startX + 475, startY, 'sms/img/router.png');
        let clientsNode = this.makeDashedNode('client组', startX + 1000, startY, 'sms/img/client.png')
        let batSaveNode = this.makeDashedNode("batsave组", startX + 1550, startY, 'sms/img/save.png');

        let mqContainer = this.makeGridContainer("RabbitMQ",
            startX + 200,
            startY - spqQueue.length*50/2 + serverNode1.height/2,
            120,
            spqQueue.length*50,
            1,
            spqQueue.length);

        let redisContainer = this.makeGridContainer("Redis",
            startX + 650,
            startY - chanTask.length*50/2 + processNode.height/2,
            200,
            chanTask.length*50,
            1,
            chanTask.length);

        let mqRptContainer = this.makeGridContainer("RabbitMQ",
            startX +1150,
            startY - saveRep.length*110/2 + clientsNode.height/2,
            200,
            saveRep.length*110,
            1,
            saveRep.length
        );


        const redisNodeInArr = [];
        const redisNodeOutArr = [];

        //初始化spq:queue 和 process程序
        for(var i=0; i<spqQueue.length; i++){
            let mqNode = this.makeContainerNode(spqQueue[i]["queueName"]);
            mqContainer.add(mqNode);

            let inMQCircleNode = this.makeCircleNode("", mqNode.x, mqNode.y);
            let outMQCircleNode = this.makeCircleNode("剩余:" + spqQueue[i]["msgReady"], mqNode.x, mqNode.y);
            this.spqOutLineNodeArr.push(outMQCircleNode);

            this.makeLink(serverNode1, inMQCircleNode);

            setTimeout(function () {
                inMQCircleNode.x = mqNode.x - 65 ;
                inMQCircleNode.y = mqNode.y + mqNode.height/2- inMQCircleNode.radius;
                outMQCircleNode.x =mqNode.x + 65 + mqNode.width;
                outMQCircleNode.y =mqNode.y + mqNode.height/2- inMQCircleNode.radius;
            }, 100);

            let inLine  = this.makeLink(inMQCircleNode, mqNode, "in:" +spqQueue[i]["produceRate"]);
            let outLine = this.makeLink(mqNode, outMQCircleNode, "out:"+spqQueue[i]["consumeRate"]);

            this.spqInLineArr.push(inLine);
            this.spqOutLineArr.push(outLine);

            this.makeLink(outMQCircleNode, processNode);
        }

        //初始化redis下的chantask:queue
        for(var i=0; i<chanTask.length; i++){
            let redisNode = this.makeContainerNode(chanTask[i]["queueName"], null, 150, 30);
            redisContainer.add(redisNode);

            redisNode.meta = chanTask[i];
            redisNode.addEventListener('mouseover', function(e) {
                that.showDetail(e.target, e);
            });
            redisNode.addEventListener('mouseout', function(e) {
                that.hideDetail(e.target);
            });

            let inRedisCircleNode = this.makeCircleNode("", redisNode.x, redisNode.y);
            let outRedisCircleNode = this.makeCircleNode("剩余:" + chanTask[i]["count"], redisNode.x, redisNode.y);

            this.chanTaskNodeArr.push(redisNode);
            this.chanTaskOutNodeArr.push(outRedisCircleNode);

            redisNodeInArr.push(inRedisCircleNode);
            redisNodeOutArr.push(outRedisCircleNode);
            setTimeout(function () {
                inRedisCircleNode.x = redisNode.x - 65 ;
                inRedisCircleNode.y = redisNode.y + redisNode.height/2- inRedisCircleNode.radius;
                outRedisCircleNode.x =redisNode.x + 65 + redisNode.width;
                outRedisCircleNode.y =redisNode.y + redisNode.height/2- inRedisCircleNode.radius;
            },100);

            this.makeLink(inRedisCircleNode, redisNode, "");
            this.makeLink(redisNode, outRedisCircleNode, "");
            this.makeLink(processNode, inRedisCircleNode);
         }


        for(let i in redisNodeOutArr) {
            this.makeLink(redisNodeOutArr[i], clientsNode);
        }

        //初始化saveRptTask
        for(var i=0; i<saveRep.length; i++){

            let mqGroupContainer = this.makeGridContainer("", 0, 0, 150, 90, 1, 2, "155,155,255");
            mqRptContainer.add(mqGroupContainer);

            let rptNode = this.makeContainerNode(saveRep[i]["queueName"], "125,125,255", 120, 25, "255,255,255");
            let mtNode = this.makeContainerNode(saveMt[i]["queueName"], "125,125,255", 120, 25, "255,255,255");
            mqGroupContainer.add(rptNode);
            mqGroupContainer.add(mtNode);

            let inMQCircleNode = this.makeCircleNode("", mqGroupContainer.x, mqGroupContainer.y);
            let outMQCircleNode = this.makeCircleNode("saveRpt剩余:"+saveRep[i]["msgReady"]+"\nsaveMt 剩余:"+ saveMt[i]["msgReady"] , mqGroupContainer.x, mqGroupContainer.y);

            this.makeLink(clientsNode, inMQCircleNode);
            let repInLine = this.makeLink(inMQCircleNode, rptNode, "in:" + saveRep[i]["produceRate"]);
            let mtInLine = this.makeLink(inMQCircleNode, mtNode, "in:" + saveMt[i]["produceRate"]);
            let repOutLine = this.makeLink(rptNode, outMQCircleNode, "out:" + saveRep[i]["consumeRate"]);
            let mtOutLine = this.makeLink(mtNode, outMQCircleNode, "out:" + saveMt[i]["consumeRate"]);
            this.makeLink(outMQCircleNode, batSaveNode);

            this.saveRepOutNodeArr.push(outMQCircleNode);

            this.saveRptInLineNodeArr.push(repInLine);
            this.saveMtInLineNodeArr.push(mtInLine);
            this.saveRptOutLineNodeArr.push(repOutLine);
            this.saveMtOutLineNodeArr.push(mtOutLine);

            setTimeout(function () {
                inMQCircleNode.x = mqGroupContainer.x - 65 ;
                inMQCircleNode.y = mqGroupContainer.y + mqGroupContainer.height/2- inMQCircleNode.radius;
                outMQCircleNode.x =mqGroupContainer.x + 65 + mqGroupContainer.width;
                outMQCircleNode.y =mqGroupContainer.y + mqGroupContainer.height/2- inMQCircleNode.radius;
            }, 100);

        }

    }

    SmsMonitor.prototype.makeDashedNode = function(text, x, y, icon) {
        let clientsNode = this.makeServerNode(text, x, y,
            icon || 'sms/img/client.png');
        //A C 画虚线框
        //B D
        let dushedInteval = 25;
        let dashedNodeA = this.makeNullCircelNode(clientsNode.x - dushedInteval, clientsNode.y - dushedInteval);
        let dashedNodeB = this.makeNullCircelNode(clientsNode.x - dushedInteval, clientsNode.y + dushedInteval + clientsNode.height);
        let dashedNodeC = this.makeNullCircelNode(clientsNode.x + dushedInteval + clientsNode.width, clientsNode.y - dushedInteval);
        let dashedNodeD = this.makeNullCircelNode(clientsNode.x + dushedInteval + clientsNode.width, clientsNode.y + dushedInteval + clientsNode.height);

        this.makeDashedLink(dashedNodeA, dashedNodeB);
        this.makeDashedLink(dashedNodeA, dashedNodeC);
        this.makeDashedLink(dashedNodeB, dashedNodeD);
        this.makeDashedLink(dashedNodeD, dashedNodeC);

        return clientsNode;
    }

    SmsMonitor.prototype.makeNullCircelNode = function(x, y) {
        var node = new JTopo.CircleNode();
        node.radius = 1;
        node.alpha = 0.7;
        node.setLocation(x, y);
        this.scene.add(node);

        /*node.mouseover(function(){this.text = text;});
        node.mouseout(function(){this.text = null;});*/

        return node;
    }

    SmsMonitor.prototype.makeCircleNode = function(text, x, y, color) {
        var node = new JTopo.CircleNode();
        node.radius = 5;
        node.alpha = 0.7;
        node.fontColor = color || '0, 0, 255';
        node.text = text;
        node.setLocation(x, y);
        this.scene.add(node);
        /*node.mouseover(function(){this.text = text;});
        node.mouseout(function(){this.text = null;});*/

        return node;
    }

    SmsMonitor.prototype.makeServerNode = function(text, x, y, icon) {
        var node = new JTopo.Node();
        node.fontColor = '0,0,0';
        node.width = 48;
        node.height = 48;
        node.text = text;
        node.setLocation(x, y);

        icon && node.setImage(icon);

        this.scene.add(node);

        /*node.mouseover(function(){this.text = text;});
        node.mouseout(function(){this.text = null;});*/

        return node;
    }

    SmsMonitor.prototype.makeProcessNode = function(text, x, y, icon) {
        var node = new JTopo.Node();
        node.fontColor = '0,0,0';
        node.width = 30;
        node.height = 30;
        node.text = text;
        node.textPosition = "Middle_Right";
        node.setLocation(x, y);

        icon && node.setImage(icon);

        this.scene.add(node);

        return node;
    }

    SmsMonitor.prototype.makeContainerNode = function(text, color, width, height, fontColor) {
        var node = new JTopo.Node(text);
        node.fontColor = fontColor || '0,0,0';
        node.textPosition = "Middle_Center";
        node.borderRadius = 3;
        node.alpha = 0.7;
        node.width = width || 100;
        node.height = height || 30;
        node.fillColor = color || '190, 190, 190';
        node.font = '14px 微软雅黑';

        this.scene.add(node);
        /*node.mouseover(function(){this.text = text;});
        node.mouseout(function(){this.text = null;});*/

        return node;
    }

    SmsMonitor.prototype.makeGridContainer = function(text, x, y, width, height, gridW, gridH, color) {
        var container = new JTopo.Container(text);
        container.layout = JTopo.layout.GridLayout(gridH || 10, gridW || 1);
        container.fillColor = color || '173, 203, 255';
        container.alpha = 0.7;
        container.backColor= "#5075f9";
        container.borderRadius = 10;
        container.setBound(x, y, width, height);
        this.scene.add(container);
        return container;
    }

    SmsMonitor.prototype.makeCurveLink = function(nodeA, nodeZ) {
        var link=new JTopo.CurveLink(nodeA,nodeZ);
        link.arrowsRadius = 5;
        link.lineWidth = 1;
        link.getStartPosition = function () {
            return {x : nodeA.x + nodeA.width, y : nodeA.y + nodeA.height/2};
        };
        link.getEndPosition = function () {
            return {x : nodeZ.x, y : nodeZ.y + nodeZ.height/2};
        };
        this.scene.add(link);
        return link;
    }

    SmsMonitor.prototype.makeFoldLink = function(nodeA, nodeZ) {
        var link=new JTopo.FoldLink(nodeA,nodeZ);
        link.arrowsRadius = 5;
        link.lineWidth = 1;
        link.getStartPosition = function () {
            return {x : nodeA.x + nodeA.width, y : nodeA.y + nodeA.height/2};
        };
        link.getEndPosition = function () {
            return {x : nodeZ.x, y : nodeZ.y + nodeZ.height/2};
        };
        this.scene.add(link);
        return link;
    }

    SmsMonitor.prototype.makeDashedLink = function(nodeA, nodeZ) {
        var link=new JTopo.FoldLink(nodeA,nodeZ);
        link.arrowsRadius = 5;
        link.lineWidth = 1;
        link.dashedPattern =3;
        link.getStartPosition = function () {
            return {x : nodeA.x + nodeA.width, y : nodeA.y + nodeA.height/2};
        };
        link.getEndPosition = function () {
            return {x : nodeZ.x, y : nodeZ.y + nodeZ.height/2};
        };
        this.scene.add(link);
        return link;
    }

    SmsMonitor.prototype.makeFlexionalLink = function(nodeA, nodeZ, nodeAOffset, nodeZOffset) {
        var link=new JTopo.FlexionalLink(nodeA, nodeZ);
        link.direction = "horizontal";
        link.lineWidth = 1;
        link.arrowsRadius = 5;
        link.text = "1024";
        link.fontColor = '0,0,0';
        link.arrowsRadius = 2;
        link.getStartPosition = function () {
            return {x : (nodeAOffset || 0) + nodeA.x + nodeA.width, y : nodeA.y + nodeA.height/2};
        };
        link.getEndPosition = function () {
            return {x : (nodeZOffset || 0) + nodeZ.x, y : nodeZ.y + nodeZ.height/2};
        };
        this.scene.add(link);
        return link;
    }

    SmsMonitor.prototype.makeLink = function(nodeA, nodeZ, text, nodeAOffset, nodeZOffset) {
        var link = new JTopo.Link(nodeA, nodeZ);
        link.text = text || "";
        link.arrowsRadius = 5;
        link.fontColor="0,0,255"
        link.lineWidth = 1;
        link.getStartPosition = function () {
            return {x : nodeA.x + nodeA.width + (nodeAOffset || 0), y : nodeA.y + nodeA.height/2};
        };
        link.getEndPosition = function () {
            return {x : nodeZ.x + (nodeZOffset || 0), y : nodeZ.y + nodeZ.height/2};
        };
        this.scene.add(link);
        return link;
    }

    SmsMonitor.prototype.makeContainerLink = function(node, container) {
        var link=new JTopo.Link(node,container);
        link.arrowsRadius = 10;
        link.getStartPosition = function () {
            return {x : node.x + node.width, y : node.y + node.height/2};
        };
        link.getEndPosition = function () {
            return {x : container.x, y : node.y + node.height/2};
        };
        this.scene.add(link);
        return link;
    }

    window.SmsMonitor = SmsMonitor;

}(jQuery, window, JTopo));