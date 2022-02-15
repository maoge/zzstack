!function(JTopo) {
    function DisplayElement() {
        this.initialize = function() {
            DisplayElement.prototype.initialize.apply(this, arguments),
                this.elementType = "displayElement",
                this.x = 0,
                this.y = 0,
                this.width = 32,
                this.height = 32,
                this.visible = !0,
                this.alpha = 1,
                this.rotate = 0,
                this.scaleX = 1,
                this.scaleY = 1,
                this.strokeColor = "30,30,80", // "22,124,255",
                this.borderColor = "22,124,255",
                this.fillColor = "22,124,255",
                this.shadow = !1,
                this.shadowBlur = 5,
                this.shadowColor = "rgba(0,0,0,0.5)",
                this.shadowOffsetX = 3,
                this.shadowOffsetY = 6,
                this.transformAble = !1,
                this.zIndex = 0;
            var props = "x,y,width,height,visible,alpha,rotate,scaleX,scaleY,strokeColor,fillColor,shadow,shadowColor,shadowOffsetX,shadowOffsetY,transformAble,zIndex".split(",");
            this.serializedProperties = this.serializedProperties.concat(props)
        },
            this.initialize(),
            this.paint = function(canvas) {
                canvas.beginPath(),
                    canvas.fillStyle = "rgba(" + this.fillColor + "," + this.alpha + ")",
                    canvas.rect( - this.width / 2, -this.height / 2, this.width, this.height),
                    canvas.fill(),
                    canvas.stroke(),
                    canvas.closePath()
            },
            this.getLocation = function() {
                return {
                    x: this.x,
                    y: this.y
                }
            },
            this.setLocation = function(a, b) {
                return this.x = a,
                    this.y = b,
                    this
            },
            this.getCenterLocation = function() {
                return {
                    x: this.x + this.width / 2,
                    y: this.y + this.height / 2
                }
            },
            this.setCenterLocation = function(x, y) {
                return this.x = x - this.width / 2,
                    this.y = y - this.height / 2,
                    this
            },
            this.addNode = function(node){
                node.x = this.x;
                node.y = this.y;
                this
            },
            this.getSize = function() {
                return {
                    width: this.width,
                    height: this.height
                }
            },
            this.setSize = function(w, h) {
                return this.width = w,
                    this.height = h,
                    this
            },
            this.getBound = function() {
                return {
                    left: this.x,
                    top: this.y,
                    right: this.x + this.width,
                    bottom: this.y + this.height,
                    width: this.width,
                    height: this.height
                }
            },
            this.setBound = function(x, y, width, height) {
                return this.setLocation(x, y),
                    this.setSize(width, height),
                    this
            },
            this.getDisplayBound = function() {
                return {
                    left: this.x,
                    top: this.y,
                    right: this.x + this.width * this.scaleX,
                    bottom: this.y + this.height * this.scaleY
                }
            },
            this.getDisplaySize = function() {
                return {
                    width: this.width * this.scaleX,
                    height: this.height * this.scaleY
                }
            },
            this.getPosition = function(a) {
                var b, c = this.getBound();
                return "Top_Left" == a ? b = {
                    x: c.left,
                    y: c.top
                }: "Top_Center" == a ? b = {
                    x: this.cx,
                    y: c.top
                }: "Top_Right" == a ? b = {
                    x: c.right,
                    y: c.top
                }: "Middle_Left" == a ? b = {
                    x: c.left,
                    y: this.cy
                }: "Middle_Center" == a ? b = {
                    x: this.cx,
                    y: this.cy
                }: "Middle_Right" == a ? b = {
                    x: c.right,
                    y: this.cy
                }: "Bottom_Left" == a ? b = {
                    x: c.left,
                    y: c.bottom
                }: "Bottom_Center" == a ? b = {
                    x: this.cx,
                    y: c.bottom
                }: "Bottom_Right" == a && (b = {
                    x: c.right,
                    y: c.bottom
                }),
                    b
            }
    }
    function InteractiveElement() {
        this.initialize = function() {
            InteractiveElement.prototype.initialize.apply(this, arguments),
                this.elementType = "interactiveElement",
                this.dragable = !1,
                this.selected = !1,
                this.showSelected = !0,
                this.selectedLocation = null,
                this.isMouseOver = !1;
            var props = "dragable,selected,showSelected,isMouseOver".split(",");
            this.serializedProperties = this.serializedProperties.concat(props)
        },
            this.initialize(),
            this.paintSelected = function(canvas) {
                0 != this.showSelected && (canvas.save(), canvas.beginPath(), canvas.strokeStyle = "rgba(168,202,255, 0.9)", canvas.fillStyle = "rgba(168,202,236,0.7)", canvas.rect( - this.width / 2 - 3, -this.height / 2 - 3, this.width + 6, this.height + 6), canvas.fill(), canvas.stroke(), canvas.closePath(), canvas.restore())
            },
            this.paintMouseover = function(canvas) {
                return this.paintSelected(canvas)
            },
            this.isInBound = function(x, y) {
                return x > this.x && x < this.x + this.width * Math.abs(this.scaleX) && y > this.y && y < this.y + this.height * Math.abs(this.scaleY)
            },
            this.selectedHandler = function() {
                this.selected = !0,
                    this.selectedLocation = {
                        x: this.x,
                        y: this.y
                    }
            },
            this.unselectedHandler = function() {
                this.selected = !1,
                    this.selectedLocation = null
            },
            this.dbclickHandler = function(event) {
                this.dispatchEvent("dbclick", event)
            },
            this.clickHandler = function(event) {
                this.dispatchEvent("click", event)
            },
            this.mousedownHander = function(event) {
                this.dispatchEvent("mousedown", event)
            },
            this.mouseupHandler = function(event) {
                this.dispatchEvent("mouseup", event)
            },
            this.mouseoverHandler = function(event) {
                this.isMouseOver = !0,
                    this.dispatchEvent("mouseover", event)
            },
            this.mousemoveHandler = function(event) {
                this.dispatchEvent("mousemove", event)
            },
            this.mouseoutHandler = function(event) {
                this.isMouseOver = !1,
                    this.dispatchEvent("mouseout", event)
            },
            this.mousedragHandler = function(event) {
                var x = this.selectedLocation.x + event.dx,
                    y = this.selectedLocation.y + event.dy;
                this.setLocation(x, y),
                    this.dispatchEvent("mousedrag", event)
            },
            this.contextmenuHandler = function(event){
                this.dispatchEvent("contextmenu", event);
            },
            this.dataEventHandler = function(data){
                this.dispatchEvent("dataEvent", data);
            },
            this.addEventListener = function(topic, handler) {
                var moduler = this,
                    action = function(event) {
                        handler.call(moduler, event)
                    };
                return this.messageBus || (this.messageBus = new JTopo.util.MessageBus),
                    this.messageBus.subscribe(topic, action),
                    this
            },
            this.dispatchEvent = function(topic, event) {
                return this.messageBus ? (this.messageBus.publish(topic, event), this) : null
            },
            this.removeEventListener = function(topic) {
                this.messageBus.unsubscribe(topic)
            },
            this.removeAllEventListener = function() {
                this.messageBus = new JTopo.util.MessageBus
            };
        var topics = "click,dbclick,mousedown,mouseup,mouseover,mouseout,mousemove,mousedrag,touchstart,touchmove,touchend,dataEvent".split(","),
            moduler = this;
        topics.forEach(function(topic) {
            moduler[topic] = function(event) {
                null != event ? this.addEventListener(topic, event) : this.dispatchEvent(topic)
            }
        })
    }
    function EditableElement() {
        this.initialize = function() {
            EditableElement.prototype.initialize.apply(this, arguments),
                this.editAble = !1,
                this.selectedPoint = null
        },
            this.getCtrlPosition = function(a) {
                var offsetX = 5,
                    offsetY = 5,
                    pos = this.getPosition(a);
                return {
                    left: pos.x - offsetX,
                    top: pos.y - offsetY,
                    right: pos.x + offsetX,
                    bottom: pos.y + offsetY
                }
            },
            this.selectedHandler = function(b) {
                EditableElement.prototype.selectedHandler.apply(this, arguments),
                    this.selectedSize = {
                        width: this.width,
                        height: this.height
                    },
                b.scene.mode == JTopo.SceneMode.edit && (this.editAble = !0)
            },
            this.unselectedHandler = function() {
                EditableElement.prototype.unselectedHandler.apply(this, arguments),
                    this.selectedSize = null,
                    this.editAble = !1
            };
        var props = ["Top_Left", "Top_Center", "Top_Right", "Middle_Left", "Middle_Right", "Bottom_Left", "Bottom_Center", "Bottom_Right"];
        this.paintCtrl = function(canvas) {
            if (0 != this.editAble) {
                canvas.save();
                for (var i = 0; i < props.length; i++) {
                    var d = this.getCtrlPosition(props[i]);
                    d.left -= this.cx,
                        d.right -= this.cx,
                        d.top -= this.cy,
                        d.bottom -= this.cy;
                    var e = d.right - d.left,
                        f = d.bottom - d.top;
                    canvas.beginPath(),
                        canvas.strokeStyle = "rgba(0,0,0,0.8)",
                        canvas.rect(d.left, d.top, e, f),
                        canvas.stroke(),
                        canvas.closePath(),
                        canvas.beginPath(),
                        canvas.strokeStyle = "rgba(255,255,255,0.3)",
                        canvas.rect(d.left + 1, d.top + 1, e - 2, f - 2),
                        canvas.stroke(),
                        canvas.closePath()
                }
                canvas.restore()
            }
        },
            this.isInBound = function(x, y) {
                if (this.selectedPoint = null, 1 == this.editAble) for (var i = 0; i < props.length; i++) {
                    var pos = this.getCtrlPosition(props[i]);
                    if (x > pos.left && x < pos.right && y > pos.top && y < pos.bottom) return this.selectedPoint = props[i],
                        !0
                }
                return EditableElement.prototype.isInBound.apply(this, arguments)
            },
            this.mousedragHandler = function(event) {
                if (null == this.selectedPoint) {
                    var x = this.selectedLocation.x + event.dx,
                        y = this.selectedLocation.y + event.dy;
                    this.setLocation(x, y),
                        this.dispatchEvent("mousedrag", event)
                } else {
                    if ("Top_Left" == this.selectedPoint) {
                        var w = this.selectedSize.width - event.dx,
                            h = this.selectedSize.height - event.dy,
                            x = this.selectedLocation.x + event.dx,
                            y = this.selectedLocation.y + event.dy;
                        x < this.x + this.width && (this.x = x, this.width = w),
                        y < this.y + this.height && (this.y = y, this.height = h)
                    } else if ("Top_Center" == this.selectedPoint) {
                        var h = this.selectedSize.height - event.dy,
                            y = this.selectedLocation.y + event.dy;
                        y < this.y + this.height && (this.y = y, this.height = h)
                    } else if ("Top_Right" == this.selectedPoint) {
                        var x = this.selectedSize.width + event.dx,
                            y = this.selectedLocation.y + event.dy;
                        y < this.y + this.height && (this.y = y, this.height = this.selectedSize.height - event.dy),
                        x > 1 && (this.width = x)
                    } else if ("Middle_Left" == this.selectedPoint) {
                        var w = this.selectedSize.width - event.dx,
                            x = this.selectedLocation.x + event.dx;
                        x < this.x + this.width && (this.x = x),
                        w > 1 && (this.width = w)
                    } else if ("Middle_Right" == this.selectedPoint) {
                        var w = this.selectedSize.width + event.dx;
                        w > 1 && (this.width = w)
                    } else if ("Bottom_Left" == this.selectedPoint) {
                        var w = this.selectedSize.width - event.dx,
                            x = this.selectedLocation.x + event.dx;
                        w > 1 && (this.x = x, this.width = w);
                        var h = this.selectedSize.height + event.dy;
                        h > 1 && (this.height = h)
                    } else if ("Bottom_Center" == this.selectedPoint) {
                        var h = this.selectedSize.height + event.dy;
                        h > 1 && (this.height = h)
                    } else if ("Bottom_Right" == this.selectedPoint) {
                        var w = this.selectedSize.width + event.dx;
                        w > 1 && (this.width = w);
                        var h = this.selectedSize.height + event.dy;
                        h > 1 && (this.height = h)
                    }
                    this.dispatchEvent("resize", event)
                }
            }
    }
    DisplayElement.prototype = new JTopo.Element,
        Object.defineProperties(DisplayElement.prototype, {
            cx: {
                get: function() {
                    return this.x + this.width / 2
                },
                set: function(x) {
                    this.x = x - this.width / 2
                }
            },
            cy: {
                get: function() {
                    return this.y + this.height / 2
                },
                set: function(y) {
                    this.y = y - this.height / 2
                }
            }
        }),
        InteractiveElement.prototype = new DisplayElement,
        EditableElement.prototype = new InteractiveElement,
        JTopo.DisplayElement = DisplayElement,
        JTopo.InteractiveElement = InteractiveElement,
        JTopo.EditableElement = EditableElement
} (JTopo)