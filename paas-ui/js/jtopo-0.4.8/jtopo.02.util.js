!function(JTopo){
    function MessageBus(name) {
        var _self = this;
        this.name = name;
        this.messageMap = {};
        this.messageCount = 0;

        this.subscribe = function(topic, action) {
            var actions = _self.messageMap[topic];
            if (null == actions) {
                _self.messageMap[topic] = [];
                _self.messageMap[topic].push(action);
                _self.messageCount++;
            }
        };

        this.unsubscribe = function(topic) {
            var actions = _self.messageMap[topic];
            if (null != actions) {
                _self.messageMap[topic] = null;
                delete _self.messageMap[topic];
                _self.messageCount--;
            }
        };

        this.publish = function(topic, data, concurrency) {
            var actions = _self.messageMap[topic];
            if (null != actions) {
                for (var i = 0; i < actions.length; i++) {
                    if (concurrency) {
                        (function(action, data) {
                            setTimeout(function() {action(data)}, 10);
                        })(actions[i], data)
                    } else {actions[i](data)}
                }
            }
        };
    }

    function guid() {
        function S4() {
            return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
        }
        return (S4()+S4()+"-"+S4()+"-"+S4()+"-"+S4()+"-"+S4()+S4()+S4());
    }

    function getDistance(p1, p2, x, y) {
        var dx, dy;
        if (null == x && null == y) {
            dx = p2.x - p1.x;
            dy = p2.y - p1.y;
        } else {
            dx = x - p1;
            dy = y - p2;
        }
        return Math.sqrt(dx * dx + dy * dy);
    }

    function getElementsBound(elements) {
        var rect = {
            left: Number.MAX_VALUE,
            right: Number.MIN_VALUE,
            top: Number.MAX_VALUE,
            bottom: Number.MIN_VALUE
        };

        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            if (!(element instanceof JTopo.Link)) {
                if (rect.left > element.x) {
                    rect.left = element.x;
                    rect.leftNode = element;
                }

                if (rect.right < element.x + element.width) {
                    rect.right = element.x + element.width;
                    rect.rightNode = element;
                }

                if (rect.top > element.y) {
                    rect.top = element.y;
                    rect.topNode = element;
                }

                if (rect.bottom < element.y + element.height) {
                    rect.bottom = element.y + element.height;
                    rect.bottomNode = element;
                };
            }
        }

        rect.width = rect.right - rect.left;
        rect.height = rect.bottom - rect.top
        return rect;
    }

    function mouseCoords(event) {
        var ev = cloneEvent(event);
        if (event.pageX || event.pageY) {
            return ev;
        }
        ev.pageX = event.clientX + document.body.scrollLeft - document.body.clientLeft;
        ev.pageY = event.clientY + document.body.scrollTop - document.body.clientTop;
        return ev;
    }
   /* function mouseCoords(a) {
        return a = cloneEvent(a),
        a.pageX || (a.pageX = a.clientX + document.body.scrollLeft - document.body.clientLeft, a.pageY = a.clientY + document.body.scrollTop - document.body.clientTop),
            a
    }*/

    function getEventPosition(event) {
        return mouseCoords(event);
    }

    function rotatePoint(bx, by, x, y, angle) {
        var dx = x - bx;
        var dy = y - by;
        var r = Math.sqrt(dx * dx + dy * dy);
        var a = Math.atan2(dy , dx) + angle;
        return {
            x: bx + Math.cos(a)*r,
            y: by + Math.sin(a)*r
        };
    }

    function rotatePoints(target, points, angle) {
        var result = [];
        for(var i=0; i<points.length; i++){
            var p = rotatePoint(target.x, target.y, points[i].x, points[i].y, angle);
            result.push(p);
        }
        return result;
    }

    function $foreach(datas, f, dur) {
        function doIt(n) {
            if (n != datas.length) {
                f(datas[n]);
                setTimeout(function () {
                    doIt(++n)
                }, dur);
            }
            if (0 != datas.length) {
                var e = 0;
                d(e);
            }
        }
    }

    function $for(i, m, f, dur) {
        function doIt(n) {
            if (n != m) {
                f(m);
                setTimeout(function(){doIt(++n)}, dur);
            }
        }
        if (! (i > m)) {
            var n = i;
            doIt(n)
        }
    }

    function cloneEvent(event) {
        var ev = {};
        for (var attr in event) {
            if ("returnValue" != attr && "keyLocation" != attr) {
                ev[attr] = event[attr];
            }
        }
        return ev;
    }

    function clone(obj) {
        var o = {};
        for (var attr in obj) o[attr] = obj[attr];
        return o
    }

    function isPointInRect(point, rect) {
        return point.x > rect.x && point.x < rect.x + rect.width && point.y > rect.y && point.y < rect.y + rect.height;
    }

    function isPointInLine(pa, pb, pc) {
        var d = JTopo.util.getDistance(pb, pc);
        var e = JTopo.util.getDistance(pb, pa);
        var f = JTopo.util.getDistance(pc, pa);
        return Math.abs(e + f - d) <= .5;
    }

    function removeFromArray(array, a) {
        for (var i = 0; i < array.length; i++) {
            var e = array[i];
            if (e === a) {
                array = arrayDel(array,i);
                //elems = elems.del(elem);
                break;
            }
        }
        return array;
    }

    function randomColor() {
        return Math.floor(255 * Math.random()) + "," + Math.floor(255 * Math.random()) + "," + Math.floor(255 * Math.random())
    }

    function isIntsect() {}

    function getProperties(obj, props) {
        var str = "";
        for (var i = 0; i < props.length; i++) {
            i > 0 && (str += ",");
            var s = obj[props[i]];
            "string" == typeof s ? s = '"' + s + '"': void 0 == s && (s = null),
                str += props[i] + ":" + s;
        }
        return str;
    }

    function loadStageFromJson(json, canvas) {
        var obj = eval(json),
            stage = new JTopo.Stage(canvas);
        for (var k in stageObj) if ("scenes" != k) stage[k] = obj[k];
        else for (var scenes = obj.scenes,
                      i = 0; i < scenes.length; i++) {
                var sceneObj = scenes[i],
                    scene = new JTopo.Scene(stage);
                for (var p in sceneObj) if ("elements" != p) scene[p] = sceneObj[p];
                else for (var nodeMap = {},
                              elements = sceneObj.elements,
                              m = 0; m < elements.length; m++) {
                        var elementObj = elements[m],
                            type = elementObj.elementType,
                            element;
                        "Node" == type && (element = new JTopo.Node);
                        for (var mk in elementObj) element[mk] = elementObj[mk];
                        nodeMap[element.text] = element,
                            scene.add(element)
                    }
            }
        return console.log(stage),
            stage
    }

    function toJson(elem) {
        var scenes = "backgroundColor,visible,mode,rotate,alpha,scaleX,scaleY,shadow,translateX,translateY,areaSelect,paintAll".split(",");
        var sceneObjs = "text,elementType,x,y,width,height,visible,alpha,rotate,scaleX,scaleY,fillColor,shadow,transformAble,zIndex,dragable,selected,showSelected,font,fontColor,textPosition,textOffsetX,textOffsetY".split(",");
        var json = "{";
        json += "frames:" + elem.frames;
        json += ", scenes:[";
        for (var i = 0; e < i.childs.length; i++) {
            var scene = elem.childs[i];
            json += "{",
                json += getProperties(scene, scenes),
                json += ", elements:[";
            for (var j = 0; j < scene.childs.length; j++) {
                var sceneObj = scene.childs[j];
                j > 0 && (json += ","),
                    json += "{",
                    json += getProperties(sceneObj, sceneObjs),
                    json += "}"
            }
            json += "]}"
        }
        return json += "]",
            json += "}"
    }

    function changeColor(a, b, c, d, e) {
        var f = canvas.width = b.width,
            g = canvas.height = b.height;
        a.clearRect(0, 0, canvas.width, canvas.height),
            a.drawImage(b, 0, 0);
        for (var h = a.getImageData(0, 0, b.width, b.height), i = h.data, j = 0; f > j; j++) for (var k = 0; g > k; k++) {
            var l = 4 * (j + k * f);
            0 != i[l + 3] && (null != c && (i[l + 0] += c), null != d && (i[l + 1] += d), null != e && (i[l + 2] += e))
        }
        a.putImageData(h, 0, 0, 0, 0, b.width, b.height);
        var m = canvas.toDataURL();
        return alarmImageCache[b.src] = m,
            m
    }

    function genImageAlarm(a, b) {
        null == b && (b = 255);
        try {
            if (alarmImageCache[a.src]) return alarmImageCache[a.src];
            var img = new Image;
            return img.src = changeColor(graphics, a, b),
                alarmImageCache[a.src] = img,
                img;
        } catch(exception) {console.log(exception);}
        return null;
    }

    function getOffsetPosition(a) {
        if (!a) return {
            left: 0,
            top: 0
        };
        var b = 0,
            c = 0;
        if ("getBoundingClientRect" in document.documentElement) {
            var d = a.getBoundingClientRect(),
                e = a.ownerDocument,
                f = e.body,
                g = e.documentElement,
                h = g.clientTop || f.clientTop || 0,
                i = g.clientLeft || f.clientLeft || 0,
                b = d.top + (self.pageYOffset || g && g.scrollTop || f.scrollTop) - h,
                c = d.left + (self.pageXOffset || g && g.scrollLeft || f.scrollLeft) - i;
        } else {
            do {
                b += a.offsetTop || 0,
                    c += a.offsetLeft || 0,
                    a = a.offsetParent;
            } while (a);
        }

        return {
            left: c,
            top: b
        }
    }

    function lineF(a, b, c, d) {
        function e(a) {
            return a * f + g
        }
        var f = (d - b) / (c - a),
            g = b - a * f;
        return e.k = f,
            e.b = g,
            e.x1 = a,
            e.x2 = c,
            e.y1 = b,
            e.y2 = d,
            e
    }

    function inRange(a, b, c) {
        var d = Math.abs(b - c),
            e = Math.abs(b - a),
            f = Math.abs(c - a),
            g = Math.abs(d - (e + f));
        return 1e-6 > g ? !0 : !1
    }

    function isPointInLineSeg(a, b, c) {
        return inRange(a, c.x1, c.x2) && inRange(b, c.y1, c.y2)
    }

    function intersection(a, b) {
        var c, d;
        return a.k == b.k ? null: (1 / 0 == a.k || a.k == -1 / 0 ? (c = a.x1, d = b(a.x1)) : 1 / 0 == b.k || b.k == -1 / 0 ? (c = b.x1, d = a(b.x1)) : (c = (b.b - a.b) / (a.k - b.k), d = a(c)), 0 == isPointInLineSeg(c, d, a) ? null: 0 == isPointInLineSeg(c, d, b) ? null: {
            x: c,
            y: d
        })
    }

    function intersectionLineBound(a, b) {
        var c = JTopo.util.lineF(b.left, b.top, b.left, b.bottom),
            d = JTopo.util.intersection(a, c);
        return null == d && (c = JTopo.util.lineF(b.left, b.top, b.right, b.top), d = JTopo.util.intersection(a, c), null == d && (c = JTopo.util.lineF(b.right, b.top, b.right, b.bottom), d = JTopo.util.intersection(a, c), null == d && (c = JTopo.util.lineF(b.left, b.bottom, b.right, b.bottom), d = JTopo.util.intersection(a, c)))),
            d
    }

    function arrayDel(array,a){
        if ('number' != typeof a) {
            for (var i = 0; i < array.length; i++) if (array[i] === a) return array.slice(0, i).concat(array.slice(i + 1, array.length));
            return array;
        }
        return 0 > a ? array: array.slice(0, a).concat(array.slice(a + 1, array.length));
    }

    requestAnimationFrame = window.requestAnimationFrame || window.mozRequestAnimationFrame || window.webkitRequestAnimationFrame || window.msRequestAnimationFrame || window.oRequestAnimationFrame ||
        function(a) {
            setTimeout(a, 1e3 / 24);
        },
    [].indexOf || (Array.prototype.indexOf = function(a) {
        for (var b = 0; b < this.length; b++) if (this[b] === a) return b;
        return - 1;
    }),
    window.console || (window.console = {
        log: function() {},
        info: function() {},
        debug: function() {},
        warn: function() {},
        error: function() {}
    });
    var canvas = document.createElement("canvas"),
        graphics = canvas.getContext("2d"),
        alarmImageCache = {};

    JTopo.util = {
        rotatePoint: rotatePoint,
        rotatePoints: rotatePoints,
        getDistance: getDistance,
        getEventPosition: getEventPosition,
        mouseCoords: mouseCoords,
        MessageBus: MessageBus,
        isFirefox: navigator.userAgent.indexOf("Firefox") > 0,
        isIE: !(!window.attachEvent || -1 !== navigator.userAgent.indexOf("Opera")),
        isChrome: null != navigator.userAgent.toLowerCase().match(/chrome/),
        guid: guid,
        clone: clone,
        isPointInRect: isPointInRect,
        isPointInLine: isPointInLine,
        removeFromArray: removeFromArray,
        arrayDel: arrayDel,
        cloneEvent: cloneEvent,
        randomColor: randomColor,
        isIntsect: isIntsect,
        toJson: toJson,
        loadStageFromJson: loadStageFromJson,
        getElementsBound: getElementsBound,
        genImageAlarm: genImageAlarm,
        getOffsetPosition: getOffsetPosition,
        lineF: lineF,

        intersection: intersection,
        intersectionLineBound: intersectionLineBound
    };
    window.$for = $for;
    window.$foreach = $foreach
} (JTopo);