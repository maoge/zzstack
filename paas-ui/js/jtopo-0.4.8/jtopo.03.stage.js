!function(JTopo) {
    function eagleEye(stage) {
        return {
            hgap: 16,
            visible: !1,
            exportCanvas: document.createElement("canvas"),
            getImage: function(w, h) {
                var rect = stage.getBound();
                    //percentX = 1,
                    //percentY = 1;
                this.exportCanvas.width = stage.canvas.width,
                    this.exportCanvas.height = stage.canvas.height,
                    null != w && null != h ? (this.exportCanvas.width = w, this.exportCanvas.height = h)//, percentX = w / rect.width, percentY = h / rect.height)
                        : (rect.width > stage.canvas.width && (this.exportCanvas.width = rect.width), rect.height > stage.canvas.height && (this.exportCanvas.height = rect.height));
                var canvasContext = this.exportCanvas.getContext("2d");
                return stage.childs.length > 0 && (canvasContext.save(),
                    canvasContext.clearRect(0, 0, this.exportCanvas.width, this.exportCanvas.height),
                    stage.childs.forEach(function(scene) {
                        1 == scene.visible && (scene.save(),
                            scene.translateX = 0, scene.translateY = 0,
                            scene.scaleX = 1, scene.scaleY = 1, canvasContext.scale(e, f),
                            rect.left < 0 && (scene.translateX = Math.abs(rect.left)),
                            rect.top < 0 && (scene.translateY = Math.abs(rect.top)),
                                scene.paintAll = !0, scene.repaint(canvasContext),
                                scene.paintAll = !1, scene.restore())
                    }), canvasContext.restore()),
                    this.exportCanvas.toDataURL("image/png")
            },
            canvas: document.createElement("canvas"),
            update: function() {
                this.eagleImageDatas = this.getData(stage)
            },
            setSize: function(w, h) {
                this.width = this.canvas.width = w,
                    this.height = this.canvas.height = h
            },
            getData: function(w, h) {
                function translate(scene) {
                    var w = scene.stage.canvas.width,
                        h = scene.stage.canvas.height,
                        offsetX = w / scene.scaleX / 2,
                        offsetY = h / scene.scaleY / 2;
                    return {
                        translateX: scene.translateX + offsetX - offsetX * scene.scaleX,
                        translateY: scene.translateY + offsetY - offsetY * scene.scaleY
                    }
                }
                //null != j && null != k ? this.setSize(w, h) : this.setSize(200, 160);
                var canvasContext = this.canvas.getContext("2d");
                if (stage.childs.length > 0) {
                    canvasContext.save(),
                        canvasContext.clearRect(0, 0, this.canvas.width, this.canvas.height),
                        stage.childs.forEach(function(scene) {
                            1 == scene.visible && (scene.save(), scene.centerAndZoom(null, null, canvasContext), scene.repaint(canvasContext), scene.restore())
                        });
                    var f = translate(a.childs[0]),
                        x = f.translateX * (this.canvas.width / stage.canvas.width) * stage.childs[0].scaleX,
                        y = f.translateY * (this.canvas.height / stage.canvas.height) * stage.childs[0].scaleY,
                        rect = stage.getBound(),
                        percentX = stage.canvas.width / stage.childs[0].scaleX / i.width,
                        percentY = stage.canvas.height / stage.childs[0].scaleY / i.height;
                    percentX > 1 && (percentX = 1),
                    percentY > 1 && (percentY = 1),
                        x *= percentX,
                        y *= percentY,
                    rect.left < 0 && (g -= Math.abs(rect.left) * (this.width / rect.width)),
                    rect.top < 0 && (h -= Math.abs(rect.top) * (this.height / rect.height)),
                        canvasContext.save(),
                        canvasContext.lineWidth = 1,
                        canvasContext.strokeStyle = "rgba(255,0,0,1)",
                        canvasContext.strokeRect( - x, -y, e.canvas.width * percentX, canvasContext.canvas.height * percentY),
                        canvasContext.restore();
                    var image = null;
                    try {
                        image = canvasContext.getImageData(0, 0, canvasContext.canvas.width, canvasContext.canvas.height)
                    } catch(exception) {}
                    return image
                }
                return null
            },
            paint: function() {
                if (null != this.eagleImageDatas) {
                    var graph = stage.graphics;
                    graph.save(),
                        graph.fillStyle = "rgba(211,211,211,0.3)",
                        graph.fillRect(a.canvas.width - this.canvas.width - 2 * this.hgap, stage.canvas.height - this.canvas.height - 1, stage.canvas.width - this.canvas.width, this.canvas.height + 1),
                        graph.fill(),
                        graph.save(),
                        graph.lineWidth = 1,
                        graph.strokeStyle = "rgba(0,0,0,1)",
                        graph.rect(a.canvas.width - this.canvas.width - 2 * this.hgap, stage.canvas.height - this.canvas.height - 1, stage.canvas.width - this.canvas.width, this.canvas.height + 1),
                        graph.stroke(),
                        graph.restore(),
                        graph.putImageData(this.eagleImageDatas, stage.canvas.width - this.canvas.width - this.hgap, stage.canvas.height - this.canvas.height),
                        graph.restore()
                } else this.eagleImageDatas = this.getData(stage)
            },
            eventHandler: function(a, b, c) {
                var d = b.x,
                    e = b.y;
                if (d > c.canvas.width - this.canvas.width && e > c.canvas.height - this.canvas.height) {
                    if (d = b.x - this.canvas.width, e = b.y - this.canvas.height, "mousedown" == a && (this.lastTranslateX = c.childs[0].translateX, this.lastTranslateY = c.childs[0].translateY), "mousedrag" == a && c.childs.length > 0) {
                        var f = b.dx,
                            g = b.dy,
                            h = c.getBound(),
                            i = this.canvas.width / c.childs[0].scaleX / h.width,
                            j = this.canvas.height / c.childs[0].scaleY / h.height;
                            c.childs[0].translateX = this.lastTranslateX - f / i,
                            c.childs[0].translateY = this.lastTranslateY - g / j
                    }
                } else;
            }
        }
    }
    function Stage(canvas) {
        function screenToClient(event) {
            var eventPos = JTopo.util.getEventPosition(event),
                offset = JTopo.util.getOffsetPosition(n.canvas);
            return eventPos.offsetLeft = eventPos.pageX - offset.left,
                eventPos.offsetTop = eventPos.pageY - offset.top,
                eventPos.x = eventPos.offsetLeft,
                eventPos.y = eventPos.offsetTop,
                eventPos.target = null,
                eventPos
        }
        function mouseover(event) {
            document.onselectstart = function() {
                return ! 1
            },
                this.mouseOver = !0;
            var translate = screenToClient(event);
            n.dispatchEventToScenes("mouseover", translate),
                n.dispatchEvent("mouseover", translate)
        }
        function mouseout(event) {
            p = setTimeout(function() {
                    o = !0
                },
                500),
                document.onselectstart = function() {
                    return ! 0
                };
            var translate = screenToClient(event);
            n.dispatchEventToScenes("mouseout", translate),
                n.dispatchEvent("mouseout", translate),
                n.needRepaint = 0 == n.animate ? !1 : !0
        }
        function mousedown(event) {
            var translate = screenToClient(event);
            n.mouseDown = !0,
                n.mouseDownX = translate.x,
                n.mouseDownY = translate.y,
                n.dispatchEventToScenes("mousedown", translate),
                n.dispatchEvent("mousedown", translate)
        }
        function mouseup(event) {
            var translate = screenToClient(event);
            n.dispatchEventToScenes("mouseup", translate),
                n.dispatchEvent("mouseup", translate),
                n.mouseDown = !1,
                n.needRepaint = 0 == n.animate ? !1 : !0
        }
        function mousedrag(event) {
            p && (window.clearTimeout(p), p = null),
                o = !1;
            var translate = screenToClient(event);
            n.mouseDown ? 0 == event.button && (translate.dx = translate.x - n.mouseDownX, translate.dy = translate.y - n.mouseDownY, n.dispatchEventToScenes("mousedrag", translate), n.dispatchEvent("mousedrag", translate), 1 == n.eagleEye.visible && n.eagleEye.update()) : (n.dispatchEventToScenes("mousemove", translate), n.dispatchEvent("mousemove", translate))
        }
        function click(event) {
            var translate = screenToClient(event);
            n.dispatchEventToScenes("click", translate),
                n.dispatchEvent("click", translate)
        }
        function dbclick(event) {
            var translate = screenToClient(event);
            n.dispatchEventToScenes("dbclick", translate),
                n.dispatchEvent("dbclick", translate)
        }
        function mousewheel(event) {
            var translate = screenToClient(event);
            n.dispatchEventToScenes("mousewheel", translate),
                n.dispatchEvent("mousewheel", translate),
            null != n.wheelZoom && (event.preventDefault ? event.preventDefault() : (a = a || window.event, event.returnValue = !1), 1 == n.eagleEye.visible && n.eagleEye.update())
        }
        function contextmenu(event){
            var translate = screenToClient(event);
            n.dispatchEventToScenes("contextmenu", translate),
                n.dispatchEvent("contextmenu", translate)
        }
        function init(canvas) {
            JTopo.util.isIE || !window.addEventListener ? (canvas.onmouseout = mouseout, canvas.onmouseover = mouseover, canvas.onmousedown = mousedown, canvas.onmouseup = mouseup, canvas.onmousemove = mousedrag, canvas.onclick = click, canvas.ondblclick = dbclick, canvas.onmousewheel = mousewheel, canvas.touchstart = mousedown, canvas.touchmove = mousedrag, canvas.touchend = mouseup, canvas.contextMenu = contextmenu) : (canvas.addEventListener("mouseout", mouseout), canvas.addEventListener("mouseover", mouseover), canvas.addEventListener("mousedown", mousedown), canvas.addEventListener("mouseup", mouseup), canvas.addEventListener("mousemove", mousedrag), canvas.addEventListener("click", click), canvas.addEventListener("dblclick", dbclick), JTopo.util.isFirefox ? canvas.addEventListener("DOMMouseScroll", mousewheel) : canvas.addEventListener("mousewheel", mousewheel),canvas.addEventListener("contextmenu",contextmenu)),
            window.addEventListener && (window.addEventListener("keydown",
                function(event) {
                    n.dispatchEventToScenes("keydown", JTopo.util.cloneEvent(event));
                    var c = event.keyCode; (37 == c || 38 == c || 39 == c || 40 == c) && (event.preventDefault ? event.preventDefault() : (event = event || window.event, event.returnValue = !1))
                },
                !0), window.addEventListener("keyup",
                function(event) {
                    n.dispatchEventToScenes("keyup", JTopo.util.cloneEvent(event));
                    var c = event.keyCode; (37 == c || 38 == c || 39 == c || 40 == c) && (event.preventDefault ? event.preventDefault() : (event = event || window.event, event.returnValue = !1))
                },
                !0))
        }
        JTopo.stage = this;
        var n = this;
        this.initialize = function(canvas) {
            init(canvas),
                this.canvas = canvas,
                this.graphics = canvas.getContext("2d"),
                this.childs = [],
                this.frames = 24,
                this.messageBus = new JTopo.util.MessageBus,
                this.eagleEye = eagleEye(this),
                this.wheelZoom = null,
                this.mouseDownX = 0,
                this.mouseDownY = 0,
                this.mouseDown = !1,
                this.mouseOver = !1,
                this.needRepaint = !0,
                this.serializedProperties = ["frames", "wheelZoom"]
        },
        null != canvas && this.initialize(canvas);
        var o = !0,
            p = null;
        document.oncontextmenu = function() {
            return o
        },
            this.dispatchEventToScenes = function(eventHandle, event) {
                if (0 != this.frames && (this.needRepaint = !0), 1 == this.eagleEye.visible && -1 != eventHandle.indexOf("mouse")) {
                    var x = event.x,
                        y = event.y;
                    if (x > this.width - this.eagleEye.width && y > this.height - this.eagleEye.height) return void this.eagleEye.eventHandler(eventHandle, event, this)
                }
                this.childs.forEach(function(scene) {
                    if (1 == scene.visible) {
                        var handle = scene[eventHandle + "Handler"];
                        if (null == handle) throw new Error("Function not found:" + eventHandle + "Handler");
                        handle.call(scene, event)
                    }
                })
            },
            this.add = function(scene) {
                for (var i = 0; i < this.childs.length; i++) if (this.childs[i] === scene) return;
                scene.addTo(this),
                    this.childs.push(scene)
            },
            this.remove = function(scene) {
                if (null == scene) throw new Error("Stage.remove出错: 参数为null!");
                for (var i = 0; i < this.childs.length; i++) if (this.childs[i] === scene) return scene.stage = null,
                    this.childs = JTopo.util.arrayDel(this.childs,i);
                this;
                return this
            },
            this.clear = function() {
                this.childs = []
            },
            this.addEventListener = function(topic, handle) {
                var module = this,
                    action = function(event) {
                        handle.call(module, event)
                    };
                return this.messageBus.subscribe(topic, action),
                    this
            },
            this.removeEventListener = function(topic) {
                this.messageBus.unsubscribe(topic)
            },
            this.removeAllEventListener = function() {
                this.messageBus = new JTopo.util.MessageBus
            },
            this.dispatchEvent = function(topic, data) {
                return this.messageBus.publish(topic, data),
                    this
            };
        var topics = "click,dbclick,mousedown,mouseup,mouseover,mouseout,mousemove,mousedrag,mousewheel,touchstart,touchmove,touchend,keydown,keyup,contextmenu".split(","),
            module = this;
        topics.forEach(function(topic) {
            module[topic] = function(handle) {
                null != handle ? this.addEventListener(topic, handle) : this.dispatchEvent(topic)
            }
        }),
            this.dataEvent = function(data){
                this.dispatchEventToScenes("dataEvent", data),
                    this.dispatchEvent("dataEvent", data);
            },
            this.saveImageInfo = function(width, height) {
                var image = this.eagleEye.getImage(width, height),
                    win = window.open("about:blank");
                return win.document.write("<img src='" + image + "' alt='from canvas'/>"),
                    this
            },
            this.saveAsLocalImage = function(a, b) {
                var c = this.eagleEye.getImage(a, b);
                return c.replace("image/png", "image/octet-stream"),
                    window.location.href = c,
                    this
            },
            this.paint = function() {
                null != this.canvas && (this.graphics.save(), this.graphics.clearRect(0, 0, this.width, this.height), this.childs.forEach(function(a) {
                    1 == a.visible && a.repaint(n.graphics)
                }), 1 == this.eagleEye.visible && this.eagleEye.paint(this), this.graphics.restore())
            },
            this.repaint = function() {
                0 != this.frames && (this.frames < 0 && 0 == this.needRepaint || (this.paint(), this.frames < 0 && (this.needRepaint = !1)))
            },
            this.zoom = function(a) {
                this.childs.forEach(function(b) {
                    0 != b.visible && b.zoom(a)
                })
            },
            this.zoomOut = function(a) {
                this.childs.forEach(function(b) {
                    0 != b.visible && b.zoomOut(a)
                })
            },
            this.zoomIn = function(a) {
                this.childs.forEach(function(b) {
                    0 != b.visible && b.zoomIn(a)
                })
            },
            this.centerAndZoom = function() {
                this.childs.forEach(function(a) {
                    0 != a.visible && a.centerAndZoom()
                })
            },
            this.setCenter = function(a, b) {
                var c = this;
                this.childs.forEach(function(d) {
                    var e = a - c.canvas.width / 2,
                        f = b - c.canvas.height / 2;
                    d.translateX = -e,
                        d.translateY = -f
                })
            },
            this.getBound = function() {
                var a = {
                    left: Number.MAX_VALUE,
                    right: Number.MIN_VALUE,
                    top: Number.MAX_VALUE,
                    bottom: Number.MIN_VALUE
                };
                return this.childs.forEach(function(b) {
                    var c = b.getElementsBound();
                    c.left < a.left && (a.left = c.left, a.leftNode = c.leftNode),
                    c.top < a.top && (a.top = c.top, a.topNode = c.topNode),
                    c.right > a.right && (a.right = c.right, a.rightNode = c.rightNode),
                    c.bottom > a.bottom && (a.bottom = c.bottom, a.bottomNode = c.bottomNode)
                }),
                    a.width = a.right - a.left,
                    a.height = a.bottom - a.top,
                    a
            },
            this.toJson = function() {
                {
                    var b = this,
                        c = '{"version":"' + a.version + '",';
                    this.serializedProperties.length
                }
                return this.serializedProperties.forEach(function(a) {
                    var child = b[a];
                    "string" == typeof child && (child = '"' + child + '"'),
                        c += '"' + a + '":' + child + ","
                }),
                    c += '"childs":[',
                    this.childs.forEach(function(a) {
                        c += a.toJson()
                    }),
                    c += "]",
                    c += "}"
            },
            function() {
                0 == n.frames ? setTimeout(arguments.callee, 100) : n.frames < 0 ? (n.repaint(), setTimeout(arguments.callee, 1e3 / -n.frames)) : (n.repaint(), setTimeout(arguments.callee, 1e3 / n.frames))
            } (),
            setTimeout(function() {
                    n.mousewheel(function(a) {
                        var b = null == a.wheelDelta ? a.detail: a.wheelDelta;
                        null != this.wheelZoom && (b > 0 ? this.zoomIn(this.wheelZoom) : this.zoomOut(this.wheelZoom))
                    }),
                        n.paint()
                },
                300),
            setTimeout(function() {
                    n.paint()
                },
                1e3),
            setTimeout(function() {
                    n.paint()
                },
                3e3)
    }
    Stage.prototype = {
        get width() {
            return this.canvas.width
        },
        get height() {
            return this.canvas.height
        },
        set cursor(a) {
            this.canvas.style.cursor = a
        },
        get cursor() {
            return this.canvas.style.cursor
        },
        set mode(a) {
            this.childs.forEach(function(b) {
                b.mode = a
            })
        }
    },
        JTopo.Stage = Stage
} (JTopo);