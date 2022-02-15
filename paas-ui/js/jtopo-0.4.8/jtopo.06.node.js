!function(JTopo) {
    function AlarmNode(context) {
        this.initialize = function(text) {
            AlarmNode.prototype.initialize.apply(this, arguments),
                this.elementType = "node",
                this.zIndex = JTopo.zIndex_Node,
                this.text = text,
                this.font = "12px Consolas",
                this.fontColor = "255,255,255",
                this.borderWidth = 0,
                this.borderColor = "255,255,255",
                this.borderRadius = null,
                this.dragable = !0,
                this.textPosition = "Bottom_Center",
                this.textOffsetX = 0,
                this.textOffsetY = 0,
                this.transformAble = !0,
                this.inLinks = null,
                this.outLinks = null,
                this.alarmState = 0,
                this.alarmStyle = "";
            var props = "text,font,fontColor,textPosition,textOffsetX,textOffsetY,borderRadius".split(",");
            this.serializedProperties = this.serializedProperties.concat(props)
        },
            this.initialize(context),
            this.paint = function(canvas) {
                if (this.image) {
                    this.paintImage(canvas);
                } else canvas.beginPath(),
                    canvas.fillStyle = "rgba(" + this.fillColor + "," + this.alpha + ")",
                    null == this.borderRadius || 0 == this.borderRadius ? canvas.rect( - this.width / 2, -this.height / 2, this.width, this.height) : canvas.JTopoRoundRect( - this.width / 2, -this.height / 2, this.width, this.height, this.borderRadius),
                    canvas.fill(),
                    canvas.closePath();
                this.paintText(canvas),
                    this.paintBorder(canvas),
                    this.paintCtrl(canvas),
                    this.paintAlarmText(canvas)
            },
            this.paintImage = function(canvas) {
            	var alpha = canvas.globalAlpha;
                canvas.globalAlpha = this.alpha;
                if (null != this.alarm && null != this.image.alarm) {
                	if (this.alarmStyle == "flash") {
                		this.alarmState++;
                    	if (this.alarmState >= 20) {
                    		canvas.drawImage(this.image, -this.width / 2, -this.height / 2, this.width, this.height);
                    		this.alarmState = -this.alarmState;
                    	} else if (this.alarmState >= 0) {
                    		canvas.drawImage(this.image.alarm, -this.width / 2, -this.height / 2, this.width, this.height);
                    	} else {
                    		canvas.drawImage(this.image, -this.width / 2, -this.height / 2, this.width, this.height);
                    	}
                	} else {
                		canvas.drawImage(this.image.alarm, -this.width / 2, -this.height / 2, this.width, this.height);
                	}
                } else {
                	canvas.drawImage(this.image, -this.width / 2, -this.height / 2, this.width, this.height);
                }
                canvas.globalAlpha = alpha;
            },
            this.paintAlarmText = function(canvas) {
                if (null != this.alarm && "" != this.alarm) {
                    var alarmColor = this.alarmColor || "255,0,0",
                        alarmAlpha = this.alarmAlpha || .5;
                    canvas.beginPath(),
                        canvas.font = this.alarmFont || "10px 微软雅黑";
                    var alarmTextOffsetX = canvas.measureText(this.alarm).width + 6,
                        alarmTextOffsetY = canvas.measureText("田").width + 6,
                        width = this.width / 2 - alarmTextOffsetX / 2,
                        height = -this.height / 2 - alarmTextOffsetY - 8;
                    canvas.strokeStyle = "rgba(" + alarmColor + ", " + alarmAlpha + ")",
                        canvas.fillStyle = "rgba(" + alarmColor + ", " + alarmAlpha + ")",
                        canvas.lineCap = "round",
                        canvas.lineWidth = 1,
                        canvas.moveTo(width, height),
                        canvas.lineTo(width + alarmTextOffsetX, height),
                        canvas.lineTo(width + alarmTextOffsetX, height + alarmTextOffsetY),
                        canvas.lineTo(width + alarmTextOffsetX / 2 + 6, height + alarmTextOffsetY),
                        canvas.lineTo(width + alarmTextOffsetX / 2, height + alarmTextOffsetY + 8),
                        canvas.lineTo(width + alarmTextOffsetX / 2 - 6, height + alarmTextOffsetY),
                        canvas.lineTo(width, height + alarmTextOffsetY),
                        canvas.lineTo(width, height),
                        canvas.fill(),
                        canvas.stroke(),
                        canvas.closePath(),
                        canvas.beginPath(),
                        canvas.strokeStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")",
                        canvas.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")",
                        canvas.fillText(this.alarm, width + 2, height + alarmTextOffsetY - 4),
                        canvas.closePath()
                }
            },
            this.paintText = function(canvas) {
                //扩展text换行
                var context = this.text,
                    contextArr = ("" + context).split('\n');

                if(contextArr.length >1 ) {

                    var offsetX = canvas.measureText(context).width,
                        offsetY = canvas.measureText("田").width;

                    canvas.beginPath(),
                        canvas.font = this.font,
                        canvas.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")";
                        canvas.wrapText(this.text,this.height/2 + offsetX,this.height + offsetY,("rgba(" + this.fontColor + ", " + this.alpha + ")"));
                    canvas.closePath();
                    return;
                }else if (null != context && "" != context) {
                    canvas.beginPath(),
                        canvas.font = this.font;
                    var offsetX = canvas.measureText(context).width,
                        offsetY = canvas.measureText("田").width;
                    if (!this.textWidth || this.textWidth!=offsetX) {
                        this.textWidth = offsetX;
                        if (this.parentContainer) {
                        	this.parentContainer.changed = true;
                        }
                    }
                    canvas.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")";
                    var pos = this.getTextPostion(this.textPosition, offsetX, offsetY);
                    canvas.fillText(context, pos.x, pos.y),
                        canvas.closePath();
                }
            },
            this.paintBorder = function(canvas) {
                if (0 != this.borderWidth) {
                    canvas.beginPath(),
                        canvas.lineWidth = this.borderWidth,
                        canvas.strokeStyle = "rgba(" + this.borderColor + "," + this.alpha + ")";
                    var width = this.borderWidth / 2;
                    null == this.borderRadius || 0 == this.borderRadius ? canvas.rect( - this.width / 2 - width, -this.height / 2 - width, this.width + this.borderWidth, this.height + this.borderWidth) : canvas.JTopoRoundRect( - this.width / 2 - width, -this.height / 2 - width, this.width + this.borderWidth, this.height + this.borderWidth, this.borderRadius),
                        canvas.stroke(),
                        canvas.closePath()
                }
            },
            this.getTextPostion = function(style, x, y) {
                var pos = null;
                return null == style || "Bottom_Center" == style ? pos = {
                    x: -this.width / 2 + (this.width - x) / 2,
                    y: this.height / 2 + y
                }: "Top_Center" == style ? pos = {
                    x: -this.width / 2 + (this.width - x) / 2,
                    y: -this.height / 2 - y / 2
                }: "Top_Right" == style ? pos = {
                    x: this.width / 2,
                    y: -this.height / 2 - y / 2
                }: "Top_Left" == style ? pos = {
                    x: -this.width / 2 - x,
                    y: -this.height / 2 - y / 2
                }: "Bottom_Right" == style ? pos = {
                    x: this.width / 2,
                    y: this.height / 2 + y
                }: "Bottom_Left" == style ? pos = {
                    x: -this.width / 2 - x,
                    y: this.height / 2 + y
                }: "Middle_Center" == style ? pos = {
                    x: -this.width / 2 + (this.width - x) / 2,
                    y: y / 2
                }: "Middle_Right" == style ? pos = {
                    x: this.width / 2,
                    y: y / 2
                }: "Middle_Left" == style && (pos = {
                    x: -this.width / 2 - x,
                    y: y / 2
                }),
                null != this.textOffsetX && (pos.x += this.textOffsetX),
                null != this.textOffsetY && (pos.y += this.textOffsetY),
                    pos
            },
            this.setImage = function(image, type) {
                if (null == image) throw new Error("Node.setImage(): 参数Image对象为空!");
                var node = this;
                if ("string" == typeof image) {
                    var img = images[image];
                    null == img ? (img = new Image, img.src = image, img.onload = function() {
                        images[img] = img,
                        1 == type && node.setSize(img.width, img.height);
                        var alarmImg = JTopo.util.genImageAlarm(img);
                        alarmImg && (img.alarm = alarmImg),
                            node.image = img
                    }) : (type && this.setSize(img.width, img.height), this.image = img)
                } else this.image = image,
                1 == type && this.setSize(image.width, image.height)
            },
            this.removeHandler = function(a) {
                var node = this;
                this.outLinks && (this.outLinks.forEach(function(link) {
                    link.nodeA === node && a.remove(link)
                }), this.outLinks = null),
                this.inLinks && (this.inLinks.forEach(function(link) {
                    link.nodeZ === node && a.remove(link)
                }), this.inLinks = null)
            }
    }
    function Node() {
        Node.prototype.initialize.apply(this, arguments)
    }
    function TextNode(text) {
        this.initialize(),
            this.text = text,
            this.elementType = "TextNode",
            this.paint = function(canvas) {
                canvas.beginPath(),
                    canvas.font = this.font,
                    this.width = canvas.measureText(this.text).width,
                    this.height = canvas.measureText("田").width,
                    canvas.strokeStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")",
                    canvas.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")",
                    canvas.fillText(this.text, -this.width / 2, this.height / 2),
                    canvas.closePath(),
                    this.paintBorder(canvas),
                    this.paintCtrl(canvas),
                    this.paintAlarmText(canvas)
            }
    }
    function LinkNode(text, href, target) {
        this.initialize(),
            this.text = text,
            this.href = href,
            this.target = target,
            this.elementType = "LinkNode",
            this.isVisited = !1,
            this.visitedColor = null,
            this.paint = function(canvas) {
                canvas.beginPath(),
                    canvas.font = this.font,
                    this.width = canvas.measureText(this.text).width,
                    this.height = canvas.measureText("田").width,
                    this.isVisited && null != this.visitedColor ? (canvas.strokeStyle = "rgba(" + this.visitedColor + ", " + this.alpha + ")", canvas.fillStyle = "rgba(" + this.visitedColor + ", " + this.alpha + ")") : (canvas.strokeStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")", canvas.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")"),
                    canvas.fillText(this.text, -this.width / 2, this.height / 2),
                this.isMouseOver && (canvas.moveTo( - this.width / 2, this.height), canvas.lineTo(this.width / 2, this.height), canvas.stroke()),
                    canvas.closePath(),
                    this.paintBorder(canvas),
                    this.paintCtrl(canvas),
                    this.paintAlarmText(canvas)
            },
            this.mousemove(function() {
                var elems = document.getElementsByTagName("canvas");
                if (elems && elems.length > 0) for (var i = 0; i < elems.length; i++) elems[i].style.cursor = "pointer"
            }),
            this.mouseout(function() {
                var elems = document.getElementsByTagName("canvas");
                if (elems && elems.length > 0) for (var i = 0; i < elems.length; i++) elems[i].style.cursor = "default"
            }),
            this.click(function() {
                "_blank" == this.target ? window.open(this.href) : location = this.href,
                    this.isVisited = !0
            })
    }
    function CircleNode(text) {
        this.initialize(arguments),
            this._radius = 20,
            this.beginDegree = 0,
            this.endDegree = 2 * Math.PI,
            this.text = text,
            this.paint = function(canvas) {
                canvas.save(),
                    canvas.beginPath(),
                    canvas.fillStyle = "rgba(" + this.fillColor + "," + this.alpha + ")",
                    canvas.arc(0, 0, this.radius, this.beginDegree, this.endDegree, !0),
                    canvas.fill(),
                    canvas.closePath(),
                    canvas.restore(),
                    this.paintText(canvas),
                    this.paintBorder(canvas),
                    this.paintCtrl(canvas),
                    this.paintAlarmText(canvas)
            },
            this.paintSelected = function(canvas) {
                canvas.save(),
                    canvas.beginPath(),
                    canvas.strokeStyle = "rgba(168,202,255, 0.9)",
                    canvas.fillStyle = "rgba(168,202,236,0.7)",
                    canvas.arc(0, 0, this.radius + 3, this.beginDegree, this.endDegree, !0),
                    canvas.fill(),
                    canvas.stroke(),
                    canvas.closePath(),
                    canvas.restore()
            }
    }
    function SerialImgNode(images, delay, type) {
        this.initialize(),
            this.frameImages = images || [],
            this.frameIndex = 0,
            this.isStop = !0;
        var sleep = delay || 1e3;
        this.repeatPlay = !1;
        var serialImgNode = this;
        this.nextFrame = function() {
            if (!this.isStop && null != this.frameImages.length) {
                if (this.frameIndex++, this.frameIndex >= this.frameImages.length) {
                    if (!this.repeatPlay) return;
                    this.frameIndex = 0
                }
                this.setImage(this.frameImages[this.frameIndex], type),
                    setTimeout(function() {
                            serialImgNode.nextFrame()
                        },
                        sleep / images.length)
            }
        }
    }
    function MatrixImgNode(image, row, column, delay, offset) {
        this.initialize();
        var matrixImgNode = this;
        this.setImage(image),
            this.frameIndex = 0,
            this.isPause = !0,
            this.repeatPlay = !1;
        var sleep = delay || 1e3;
        offset = offset || 0,
            this.paint = function(canvas) {
                if (this.image) {
                    var width = this.width,
                        height = this.height;
                    canvas.save(),
                        canvas.beginPath(),
                        canvas.fillStyle = "rgba(" + this.fillColor + "," + this.alpha + ")";
                    var dy = (Math.floor(this.frameIndex / column) + offset) * height,
                        dx = Math.floor(this.frameIndex % column) * width;
                    canvas.drawImage(this.image, dx, dy, width, height, -width / 2, -height / 2, width, height),
                        canvas.fill(),
                        canvas.closePath(),
                        canvas.restore(),
                        this.paintText(canvas),
                        this.paintBorder(canvas),
                        this.paintCtrl(canvas),
                        this.paintAlarmText(canvas)
                }
            },
            this.nextFrame = function() {
                if (!this.isStop) {
                    if (this.frameIndex++, this.frameIndex >= row * column) {
                        if (!this.repeatPlay) return;
                        this.frameIndex = 0
                    }
                    setTimeout(function() {
                            matrixImgNode.isStop || matrixImgNode.nextFrame()
                        },
                        sleep / (row * column))
                }
            }
    }
    function AnimateNode() {
        var a = null;
        return a = arguments.length <= 3 ? new SerialImgNode(arguments[0], arguments[1], arguments[2]) : new MatrixImgNode(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]),
            a.stop = function() {
                a.isStop = !0
            },
            a.play = function() {
                a.isStop = !1,
                    a.frameIndex = 0,
                    a.nextFrame()
            },
            a
    }
    var images = {};
    AlarmNode.prototype = new JTopo.EditableElement,
        Node.prototype = new AlarmNode,
        TextNode.prototype = new Node,
        LinkNode.prototype = new TextNode,
        CircleNode.prototype = new Node,
        Object.defineProperties(CircleNode.prototype, {
            radius: {
                get: function() {
                    return this._radius
                },
                set: function(a) {
                    this._radius = a;
                    var b = 2 * this.radius,
                        c = 2 * this.radius;
                    this.width = b,
                        this.height = c
                }
            },
            width: {
                get: function() {
                    return this._width
                },
                set: function(a) {
                    this._radius = a / 2,
                        this._width = a
                }
            },
            height: {
                get: function() {
                    return this._height
                },
                set: function(a) {
                    this._radius = a / 2,
                        this._height = a
                }
            }
        }),
        SerialImgNode.prototype = new Node,
        MatrixImgNode.prototype = new Node,
        AnimateNode.prototype = new Node,
        JTopo.Node = Node,
        JTopo.TextNode = TextNode,
        JTopo.LinkNode = LinkNode,
        JTopo.CircleNode = CircleNode,
        JTopo.AnimateNode = AnimateNode
} (JTopo);