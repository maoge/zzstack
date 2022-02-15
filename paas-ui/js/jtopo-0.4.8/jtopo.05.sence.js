!function(JTopo) {
    function Scene(stage) {
        function fill(x, y, w, h) {
            return function(canvas) {
                canvas.beginPath(),
                    canvas.strokeStyle = "rgba(0,0,236,0.5)",
                    canvas.fillStyle = "rgba(0,0,236,0.1)",
                    canvas.rect(x, y, w, h),
                    canvas.fill(),
                    canvas.stroke(),
                    canvas.closePath()
            }
        }
        var sceneRef = this;
        this.initialize = function() {
            Scene.prototype.initialize.apply(this, arguments),
                this.messageBus = new JTopo.util.MessageBus,
                this.elementType = "scene",
                this.childs = [],
                this.zIndexMap = {},
                this.zIndexArray = [],
                this.backgroundColor = "255,255,255",
                this.visible = !0,
                this.alpha = 0,
                this.scaleX = 1,
                this.scaleY = 1,
                this.mode = JTopo.SceneMode.normal,
                this.translate = !0,
                this.translateX = 0,
                this.translateY = 0,
                this.lastTranslateX = 0,
                this.lastTranslateY = 0,
                this.mouseDown = !1,
                this.mouseDownX = null,
                this.mouseDownY = null,
                this.mouseDownEvent = null,
                this.areaSelect = !0,
                this.operations = [],
                this.selectedElements = [],
                this.paintAll = !1;
            var props = "background,backgroundColor,mode,paintAll,areaSelect,translate,translateX,translateY,lastTranslatedX,lastTranslatedY,alpha,visible,scaleX,scaleY".split(",");
            this.serializedProperties = this.serializedProperties.concat(props)
        },
            this.initialize(),
            this.setBackground = function(a) {
                this.background = a
            },
            this.addTo = function(stage) {
                this.stage !== stage && null != stage && (this.stage = stage)
            },
        null != stage && (stage.add(this), this.addTo(stage)),
            this.show = function() {
                this.visible = !0
            },
            this.hide = function() {
                this.visible = !1
            },
            this.paint = function(a) {
                if (0 != this.visible && null != this.stage) {
                    if (a.save(), this.paintBackgroud(a), a.restore(), a.save(), a.scale(this.scaleX, this.scaleY), 1 == this.translate) {
                        var b = this.getOffsetTranslate(a);
                        a.translate(b.translateX, b.translateY)
                    }
                    this.paintChilds(a),
                        a.restore(),
                        a.save(),
                        this.paintOperations(a, this.operations),
                        a.restore()
                }
            },
            this.repaint = function(a) {
                0 != this.visible && this.paint(a)
            },
            this.paintBackgroud = function(canvas) {
                null != this.background ? canvas.drawImage(this.background, 0, 0, canvas.canvas.width, canvas.canvas.height)
                    : (canvas.beginPath(), canvas.fillStyle = "rgba(" + this.backgroundColor + "," + this.alpha + ")",
                       canvas.fillRect(0, 0, canvas.canvas.width, canvas.canvas.height), canvas.closePath())
            },
            this.getDisplayedElements = function() {
                for (var elems = [], i = 0; i < this.zIndexArray.length; i++) {
                    for (var idx = this.zIndexArray[i], idxMap = this.zIndexMap[idx], j = 0; j < idxMap.length; j++) {
                        var elem = idxMap[j];
                        this.isVisiable(elem) && elems.push(elem)
                    }
                }
                return elems
            },
            this.getDisplayedNodes = function() {
                for (var nodes = [], i = 0; i < this.childs.length; i++) {
                    var node = this.childs[i];
                    node instanceof Scene.Node && this.isVisiable(node) && nodes.push(node)
                }
                return nodes
            },
            this.paintChilds = function(child) {
                for (var i = 0; i < this.zIndexArray.length; i++) {
                    for (var idx = this.zIndexArray[i], idxMap = this.zIndexMap[idx], j = 0; j < idxMap.length; j++) {
                        var elem = idxMap[j];
                        if (1 == this.paintAll || this.isVisiable(elem)) {
                            if (child.save(), 1 == elem.transformAble) {
                                var h = elem.getCenterLocation();
                                child.translate(h.x, h.y),
                                elem.rotate && child.rotate(elem.rotate),
                                    elem.scaleX && elem.scaleY ? child.scale(elem.scaleX, elem.scaleY) : elem.scaleX ? child.scale(elem.scaleX, 1) : elem.scaleY && child.scale(1, elem.scaleY)
                            }
                            1 == elem.shadow && (child.shadowBlur = elem.shadowBlur, child.shadowColor = elem.shadowColor, child.shadowOffsetX = elem.shadowOffsetX, child.shadowOffsetY = elem.shadowOffsetY),
                            elem instanceof JTopo.InteractiveElement && (elem.selected && 1 == elem.showSelected && elem.paintSelected(child), 1 == elem.isMouseOver && elem.paintMouseover(child)),
                                elem.paint(child),
                                child.restore()
                        }
                    }
                }
            },
            this.getOffsetTranslate = function(stage) {
                var w = this.stage.canvas.width,
                    h = this.stage.canvas.height;
                null != stage && "move" != stage && (w = stage.canvas.width, h = stage.canvas.height);
                var percentX = w / this.scaleX / 2,
                    percentY = h / this.scaleY / 2,
                    offsetPos = {
                        translateX: this.translateX + (percentX - percentX * this.scaleX),
                        translateY: this.translateY + (percentY - percentY * this.scaleY)
                    };
                return offsetPos
            },
            this.isVisiable = function(element) {
                if (1 != element.visible) return ! 1;
                if (element instanceof JTopo.Link) return ! 0;
                var offset = this.getOffsetTranslate(),
                    x = element.x + offset.translateX,
                    y = element.y + offset.translateY;
                x *= this.scaleX,
                    y *= this.scaleY;
                var posX = x + element.width * this.scaleX,
                    posY = y + element.height * this.scaleY;
                return x > this.stage.canvas.width || y > this.stage.canvas.height || 0 > posX || 0 > posY ? !1 : !0
            },
            this.paintOperations = function(a, b) {
                for (var c = 0; c < b.length; c++) b[c](a)
            },
            this.findElements = function(a) {
                for (var elems = [], i = 0; i < this.childs.length; i++) 1 == a(this.childs[i]) && elems.push(this.childs[i]);
                return elems
            },
            this.getElementsByClass = function(clazz) {
                return this.findElements(function(elem) {
                    return elem instanceof clazz
                })
            },
            this.addOperation = function(oper) {
                return this.operations.push(oper),
                    this
            },
            this.clearOperations = function() {
                return this.operations = [],
                    this
            },
            this.getElementByXY = function(b, c) {
                for (var d = null, e = this.zIndexArray.length - 1; e >= 0; e--) {
                    for (var f = this.zIndexArray[e], g = this.zIndexMap[f], h = g.length - 1; h >= 0; h--) {
                        var i = g[h];
                        if (i instanceof JTopo.InteractiveElement && this.isVisiable(i) && i.isInBound(b, c)) return d = i
                    }
                }
                return d
            },
            this.add = function(a) {
                this.childs.push(a),
                null == this.zIndexMap[a.zIndex] && (this.zIndexMap[a.zIndex] = [], this.zIndexArray.push(a.zIndex), this.zIndexArray.sort(function(a, b) {
                    return a - b
                })),
                    this.zIndexMap["" + a.zIndex].push(a)
            },
            this.remove = function(b) {
                this.childs = JTopo.util.removeFromArray(this.childs, b);
                var c = this.zIndexMap[b.zIndex];
                c && (this.zIndexMap[b.zIndex] = JTopo.util.removeFromArray(c, b)),
                    b.removeHandler(this)
            },
            this.clear = function() {
                var a = this;
                this.childs.forEach(function(b) {
                    b.removeHandler(a)
                }),
                    this.childs = [],
                    this.operations = [],
                    this.zIndexArray = [],
                    this.zIndexMap = {}
            },
            this.addToSelected = function(a) {
                this.selectedElements.push(a)
            },
            this.cancleAllSelected = function(a) {
                for (var b = 0; b < this.selectedElements.length; b++) this.selectedElements[b].unselectedHandler(a);
                this.selectedElements = []
            },
            this.notInSelectedNodes = function(a) {
                for (var b = 0; b < this.selectedElements.length; b++) if (a === this.selectedElements[b]) return ! 1;
                return ! 0
            },
            this.removeFromSelected = function(a) {
                for (var b = 0; b < this.selectedElements.length; b++) {
                    var c = this.selectedElements[b];
                    //a === stage && (this.selectedElements = this.selectedElements.del(b))
                    a === c && (this.selectedElements = JTopo.util.arrayDel(this.selectedElements,b))
                }
            },
            this.toSceneEvent = function(event) {
                var obj = JTopo.util.clone(event);
                if (obj.x /= this.scaleX, obj.y /= this.scaleY, 1 == this.translate) {
                    var d = this.getOffsetTranslate();
                    obj.x -= d.translateX,
                        obj.y -= d.translateY
                }
                return null != obj.dx && (obj.dx /= this.scaleX, obj.dy /= this.scaleY),
                null != this.currentElement && (obj.target = this.currentElement),
                    obj.scene = this,
                    obj
            },
            this.selectElement = function(event) {
                var b = sceneRef.getElementByXY(event.x, event.y);
                if (null != b) if (event.target = b, b.mousedownHander(event), b.selectedHandler(event), sceneRef.notInSelectedNodes(b)) event.ctrlKey || sceneRef.cancleAllSelected(),
                    sceneRef.addToSelected(b);
                else {
                    1 == event.ctrlKey && (b.unselectedHandler(), this.removeFromSelected(b));
                    for (var c = 0; c < this.selectedElements.length; c++) {
                        var d = this.selectedElements[c];
                        d.selectedHandler(event)
                    }
                } else event.ctrlKey || sceneRef.cancleAllSelected();
                this.currentElement = b
            },
            this.mousedownHandler = function(b) {
                var c = this.toSceneEvent(b);
                if (this.mouseDown = !0, this.mouseDownX = c.x, this.mouseDownY = c.y, this.mouseDownEvent = c, this.mode == JTopo.SceneMode.normal) this.selectElement(c),
                (null == this.currentElement || this.currentElement instanceof JTopo.Link) && 1 == this.translate && (this.lastTranslateX = this.translateX, this.lastTranslateY = this.translateY);
                else {
                    if (this.mode == JTopo.SceneMode.drag && 1 == this.translate) return this.lastTranslateX = this.translateX,
                        void(this.lastTranslateY = this.translateY);
                    this.mode == JTopo.SceneMode.select ? this.selectElement(c) : this.mode == JTopo.SceneMode.edit && (this.selectElement(c), (null == this.currentElement || this.currentElement instanceof JTopo.Link) && 1 == this.translate && (this.lastTranslateX = this.translateX, this.lastTranslateY = this.translateY))
                }
                sceneRef.dispatchEvent("mousedown", c)
            },
            this.mouseupHandler = function(b) {
                this.stage.cursor != JTopo.MouseCursor.normal && (this.stage.cursor = JTopo.MouseCursor.normal),
                    sceneRef.clearOperations();
                var c = this.toSceneEvent(b);
                null != this.currentElement && (c.target = sceneRef.currentElement, this.currentElement.mouseupHandler(c)),
                    this.dispatchEvent("mouseup", c),
                    this.mouseDown = !1
            },
            this.dragElements = function(b) {
                if (null != this.currentElement && 1 == this.currentElement.dragable) for (var c = 0; c < this.selectedElements.length; c++) {
                    var d = this.selectedElements[c];
                    if (0 != d.dragable) {
                        var e = JTopo.util.clone(b);
                        e.target = d,
                            d.mousedragHandler(e)
                    }
                }
            },
            this.mousedragHandler = function(b) {
                var c = this.toSceneEvent(b);
                this.mode == JTopo.SceneMode.normal ? null == this.currentElement || this.currentElement instanceof JTopo.Link ? 1 == this.translate && (this.stage.cursor = JTopo.MouseCursor.closed_hand, this.translateX = this.lastTranslateX + c.dx, this.translateY = this.lastTranslateY + c.dy) : this.dragElements(c) : this.mode == JTopo.SceneMode.drag ? 1 == this.translate && (this.stage.cursor = JTopo.MouseCursor.closed_hand, this.translateX = this.lastTranslateX + c.dx, this.translateY = this.lastTranslateY + c.dy) : this.mode == JTopo.SceneMode.select ? null != this.currentElement ? 1 == this.currentElement.dragable && this.dragElements(c) : 1 == this.areaSelect && this.areaSelectHandle(c) : this.mode == JTopo.SceneMode.edit && (null == this.currentElement || this.currentElement instanceof JTopo.Link ? 1 == this.translate && (this.stage.cursor = JTopo.MouseCursor.closed_hand, this.translateX = this.lastTranslateX + c.dx, this.translateY = this.lastTranslateY + c.dy) : this.dragElements(c)),
                    this.dispatchEvent("mousedrag", c)
            },
            this.areaSelectHandle = function(a) {
                var b = a.offsetLeft,
                    c = a.offsetTop,
                    f = this.mouseDownEvent.offsetLeft,
                    g = this.mouseDownEvent.offsetTop,
                    h = b >= f ? f: b,
                    i = c >= g ? g: c,
                    j = Math.abs(a.dx) * this.scaleX,
                    k = Math.abs(a.dy) * this.scaleY,
                    l = new fill(h, i, j, k);
                sceneRef.clearOperations().addOperation(l),
                    b = a.x,
                    c = a.y,
                    f = this.mouseDownEvent.x,
                    g = this.mouseDownEvent.y,
                    h = b >= f ? f: b,
                    i = c >= g ? g: c,
                    j = Math.abs(a.dx),
                    k = Math.abs(a.dy);
                for (var m = h + j,
                         n = i + k,
                         o = 0; o < sceneRef.childs.length; o++) {
                    var p = sceneRef.childs[o];
                    p.x > h && p.x + p.width < m && p.y > i && p.y + p.height < n && sceneRef.notInSelectedNodes(p) && (p.selectedHandler(a), sceneRef.addToSelected(p))
                }
            },
            this.mousemoveHandler = function(b) {
                this.mousecoord = {
                    x: b.x,
                    y: b.y
                };
                var c = this.toSceneEvent(b);
                if (this.mode == JTopo.SceneMode.drag) return void(this.stage.cursor = JTopo.MouseCursor.open_hand);
                this.mode == JTopo.SceneMode.normal ? this.stage.cursor = JTopo.MouseCursor.normal: this.mode == JTopo.SceneMode.select && (this.stage.cursor = JTopo.MouseCursor.normal);
                var d = sceneRef.getElementByXY(c.x, c.y);
                null != d ? (sceneRef.mouseOverelement && sceneRef.mouseOverelement !== d && (c.target = d, sceneRef.mouseOverelement.mouseoutHandler(c)), sceneRef.mouseOverelement = d, 0 == d.isMouseOver ? (c.target = d, d.mouseoverHandler(c), sceneRef.dispatchEvent("mouseover", c)) : (c.target = d, d.mousemoveHandler(c), sceneRef.dispatchEvent("mousemove", c))) : sceneRef.mouseOverelement ? (c.target = d, sceneRef.mouseOverelement.mouseoutHandler(c), sceneRef.mouseOverelement = null, sceneRef.dispatchEvent("mouseout", c)) : (c.target = null, sceneRef.dispatchEvent("mousemove", c))
            },
            this.mouseoverHandler = function(a) {
                var b = this.toSceneEvent(a);
                this.dispatchEvent("mouseover", b)
            },
            this.mouseoutHandler = function(a) {
                var b = this.toSceneEvent(a);
                this.dispatchEvent("mouseout", b)
            },
            this.clickHandler = function(a) {
                var b = this.toSceneEvent(a);
                this.currentElement && (b.target = this.currentElement, this.currentElement.clickHandler(b)),
                    this.dispatchEvent("click", b)
            },
            this.dbclickHandler = function(a) {
                var b = this.toSceneEvent(a);
                this.currentElement ? (b.target = this.currentElement, this.currentElement.dbclickHandler(b)) : sceneRef.cancleAllSelected(),
                    this.dispatchEvent("dbclick", b)
            },
            this.mousewheelHandler = function(a) {
                var b = this.toSceneEvent(a);
                this.dispatchEvent("mousewheel", b)
            },
            this.touchstart = this.mousedownHander,
            this.touchmove = this.mousedragHandler,
            this.touchend = this.mousedownHander,
            this.keydownHandler = function(a) {
                this.dispatchEvent("keydown", a)
            },
            this.keyupHandler = function(a) {
                this.dispatchEvent("keyup", a)
            },
            this.contextmenuHandler = function(event){
                var b = this.toSceneEvent(event);
                this.currentElement && (b.target = this.currentElement, this.currentElement.contextmenuHandler(b));
                this.currentElement  ||  this.dispatchEvent("contextmenu", b);
            },
            this.dataEventHandler = function(data){
                var element = this.find(function(ele){
                    return ele._id == data.id;
                });
                element.dataEventHandler(data);
                this.dispatchEvent("dataEvent", data)
            }
            this.addEventListener = function(a, b) {
                var c = this,
                    d = function(a) {
                        b.call(c, a)
                    };
                return this.messageBus.subscribe(a, d),
                    this
            },
            this.removeEventListener = function(a) {
                this.messageBus.unsubscribe(a)
            },
            this.removeAllEventListener = function() {
                this.messageBus = new JTopo.util.MessageBus
            },
            this.dispatchEvent = function(a, b) {
                return this.messageBus.publish(a, b),
                    this
            };
        var f = "click,dbclick,mousedown,mouseup,mouseover,mouseout,mousemove,mousedrag,mousewheel,touchstart,touchmove,touchend,keydown,keyup,dataEvent".split(","),
            g = this;
        return f.forEach(function(a) {
            g[a] = function(b) {
                null != b ? this.addEventListener(a, b) : this.dispatchEvent(a)
            }
        }),
            this.zoom = function(a, b) {
                null != a && 0 != a && (this.scaleX = a),
                null != b && 0 != b && (this.scaleY = b)
            },
            this.zoomOut = function(a) {
                0 != a && (null == a && (a = .8), this.scaleX /= a, this.scaleY /= a)
            },
            this.zoomIn = function(a) {
                0 != a && (null == a && (a = .8), this.scaleX *= a, this.scaleY *= a)
            },
            this.getBound = function() {
                return {
                    left: 0,
                    top: 0,
                    right: this.stage.canvas.width,
                    bottom: this.stage.canvas.height,
                    width: this.stage.canvas.width,
                    height: this.stage.canvas.height
                }
            },
            this.getElementsBound = function() {
                return JTopo.util.getElementsBound(this.childs)
            },
            this.translateToCenter = function(a) {
                var b = this.getElementsBound(),
                    c = this.stage.canvas.width / 2 - (b.left + b.right) / 2,
                    d = this.stage.canvas.height / 2 - (b.top + b.bottom) / 2;
                a && (c = a.canvas.width / 2 - (b.left + b.right) / 2, d = a.canvas.height / 2 - (b.top + b.bottom) / 2),
                    this.translateX = c,
                    this.translateY = d
            },
            this.setCenter = function(a, b) {
                var c = a - this.stage.canvas.width / 2,
                    d = b - this.stage.canvas.height / 2;
                this.translateX = -c,
                    this.translateY = -d
            },
            this.centerAndZoom = function(a, b, c) {
                if (this.translateToCenter(c), null == a || null == b) {
                    var d = this.getElementsBound(),
                        e = d.right - d.left,
                        f = d.bottom - d.top,
                        g = this.stage.canvas.width / e,
                        h = this.stage.canvas.height / f;
                    c && (g = c.canvas.width / e, h = c.canvas.height / f);
                    var i = Math.min(g, h);
                    if (i > 1) return;
                    this.zoom(i, i)
                }
                this.zoom(a, b)
            },
            this.getCenterLocation = function() {
                return {
                    x: sceneRef.stage.canvas.width / 2,
                    y: sceneRef.stage.canvas.height / 2
                }
            },
            this.doLayout = function(a) {
                a && a(this, this.childs)
            },
            this.toJson = function() {
                {
                    var scene = this,
                        str = "{";
                    this.serializedProperties.length
                }
                this.serializedProperties.forEach(function(prop) {
                    var child = scene[prop];
                    "background" == prop && (child = scene._background.src),
                    "string" == typeof child && (child = '"' + child + '"'),
                        str += '"' + prop + '":' + child + ","
                }),
                    str += '"childs":[';
                var len = this.childs.length;
                return this.childs.forEach(function(child, idx) {
                    str += child.toJson(),
                    len > idx + 1 && (str += ",")
                }),
                    str += "]",
                    str += "}"
            },
            sceneRef
    }
    Scene.prototype = new JTopo.Element;
    var imgs = {};
    Object.defineProperties(Scene.prototype, {
        background: {
            get: function() {
                return this._background
            },
            set: function(prop) {
                if ("string" == typeof prop) {
                    var img = imgs[prop];
                    null == img && (img = new Image, img.src = prop, img.onload = function() {
                        imgs[prop] = img
                    }),
                        this._background = img
                } else this._background = prop
            }
        }
    }),
        JTopo.Scene = Scene
} (JTopo);