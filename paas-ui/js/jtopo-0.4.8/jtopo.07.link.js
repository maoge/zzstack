!function(JTopo) {
    function getLinksByNodeAToB(nodeA, nodeZ) {
        var links = [];
        if (null == nodeA || null == nodeZ) return links;
        if (nodeA && nodeZ && nodeA.outLinks && nodeZ.inLinks) {
            for (var i = 0; i < nodeA.outLinks.length; i++) {
                for (var outLink = nodeA.outLinks[i], j = 0; j < nodeZ.inLinks.length; j++) {
                    var inLink = nodeZ.inLinks[j];
                    outLink === inLink && links.push(inLink);
                }
            }
        }
        return links;
    }
    function getLinksByNodeAAndB(nodeA, nodeZ) {
        var aToBLinks = getLinksByNodeAToB(nodeA, nodeZ),
            bToALinks = getLinksByNodeAToB(nodeZ, nodeA),
            links = aToBLinks.concat(bToALinks);
        return links;
    }
    function getOtherLinksByLink(link) {
        var links = getLinksByNodeAAndB(link.nodeA, link.nodeZ);
        return links = links.filter(function(otherLink) {
            return link !== otherLink
        })
    }
    function getLinksCount(nodeA, nodeZ) {
        return getLinksByNodeAAndB(nodeA, nodeZ).length
    }
    function Link(nodeA, nodeZ, text) {
        function h(b, c) {
            var d = JTopo.util.lineF(b.cx, b.cy, c.cx, c.cy),
                e = b.getBound(),
                f = JTopo.util.intersectionLineBound(d, e);
            return f
        }
        this.initialize = function(nodeA, nodeZ, text) {
            if (Link.prototype.initialize.apply(this, arguments), this.elementType = "link", this.zIndex = JTopo.zIndex_Link, 0 != arguments.length) {
                this.text = text,
                    this.nodeA = nodeA,
                    this.nodeZ = nodeZ,
                this.nodeA && null == this.nodeA.outLinks && (this.nodeA.outLinks = []),
                this.nodeA && null == this.nodeA.inLinks && (this.nodeA.inLinks = []),
                this.nodeZ && null == this.nodeZ.inLinks && (this.nodeZ.inLinks = []),
                this.nodeZ && null == this.nodeZ.outLinks && (this.nodeZ.outLinks = []),
                null != this.nodeA && this.nodeA.outLinks.push(this),
                null != this.nodeZ && this.nodeZ.inLinks.push(this),
                    this.caculateIndex(),
                    this.font = "16px Consolas",
                    this.fontColor = "0,0,0",
                    this.lineWidth = 2,
                    this.lineJoin = "miter",
                    this.transformAble = !1,
                    this.bundleOffset = 20,//折线拐角处的长度
                    this.bundleGap = 12,//线条之间的间隔
                    this.textOffsetX = 0,// 文本x偏移量
                    this.textOffsetY = 0,//// 文本y偏移量
                    this.arrowsRadius = null,////箭头大小
                    this.arrowsOffset = 0,
                    this.dashedPattern = null,// 虚线
                    this.path = [];
                var e = "text,font,fontColor,lineWidth,lineJoin".split(",");
                this.serializedProperties = this.serializedProperties.concat(e)
            }
        },
        this.caculateIndex = function() {
            //计算是第几条Line，值添加到nodeIndex
            var a = getLinksCount(this.nodeA, this.nodeZ);
            a > 0 && (this.nodeIndex = a - 1)
        },
        this.initialize(nodeA, nodeZ, text),
        this.removeHandler = function() {
            var self = this;
            this.nodeA && this.nodeA.outLinks && (this.nodeA.outLinks = this.nodeA.outLinks.filter(function(link) {
                return link !== self
            })),
            this.nodeZ && this.nodeZ.inLinks && (this.nodeZ.inLinks = this.nodeZ.inLinks.filter(function(link) {
                return link !== self
            }));
            var otherLinks = getOtherLinksByLink(this);
            otherLinks.forEach(function(link, index) {
                //更新其它link的nodeIndex
                link.nodeIndex = index;
            })
        },
        this.getStartPosition = function() {
            var nodeAPosition = {
                x: this.nodeA.cx,
                y: this.nodeA.cy
            };
            return "horizontal" == this.direction ? this.nodeZ.cx > nodeAPosition.x ? nodeAPosition.x += this.nodeA.width / 2 : nodeAPosition.x -= this.nodeA.width / 2 : this.nodeZ.cy > nodeAPosition.y ? nodeAPosition.y += this.nodeA.height / 2 : nodeAPosition.y -= this.nodeA.height / 2,
                nodeAPosition
        },
        this.getEndPosition = function() {
            var a;
            return null != this.arrowsRadius && (a = h(this.nodeZ, this.nodeA)),
            null == a && (a = {
                x: this.nodeZ.cx,
                y: this.nodeZ.cy
            }),
                "horizontal" == this.direction ? a.x = this.nodeA.cx < a.x ? this.nodeZ.x: this.nodeZ.x + this.nodeZ.width : this.nodeA.cy < a.y ? a.y -= this.nodeZ.height / 2 : a.y += this.nodeZ.height / 2,
                a
        },
        this.getPath = function() {
            //获取所有折线上的点的位置信息
            var res = [],
                startPosition = this.getStartPosition(),
                endPostition = this.getEndPosition();
            if (this.nodeA === this.nodeZ) return [startPosition, endPostition];
            var count = getLinksCount(this.nodeA, this.nodeZ);
            if (1 == count) return [startPosition, endPostition];
            var f = Math.atan2(endPostition.y - startPosition.y, endPostition.x - startPosition.x),
            g = {
                x: startPosition.x + this.bundleOffset * Math.cos(f),
                y: startPosition.y + this.bundleOffset * Math.sin(f)
            },
            h = {
                x: endPostition.x + this.bundleOffset * Math.cos(f - Math.PI),
                y: endPostition.y + this.bundleOffset * Math.sin(f - Math.PI)
            },
            i = f - Math.PI / 2,
            j = f - Math.PI / 2,
            k = count * this.bundleGap / 2 - this.bundleGap / 2,
            l = this.bundleGap * this.nodeIndex,
            m = {
                x: g.x + l * Math.cos(i),
                y: g.y + l * Math.sin(i)
            },
            n = {
                x: h.x + l * Math.cos(j),
                y: h.y + l * Math.sin(j)
            };
            return m = {
                x: m.x + k * Math.cos(i - Math.PI),
                y: m.y + k * Math.sin(i - Math.PI)
            },
            n = {
                x: n.x + k * Math.cos(j - Math.PI),
                y: n.y + k * Math.sin(j - Math.PI)
            },
            res.push({
                x: startPosition.x,
                y: startPosition.y
            }),
            res.push({
                x: m.x,
                y: m.y
            }),
            res.push({
                x: n.x,
                y: n.y
            }),
            res.push({
                x: endPostition.x,
                y: endPostition.y
            }),
            res
        },
        this.paintPath = function(graphics, path) {
            if (this.nodeA === this.nodeZ) return void this.paintLoop(graphics);
            graphics.beginPath(),
                graphics.moveTo(path[0].x, path[0].y);
            for (var i = 1; i < path.length; i++) null == this.dashedPattern ? graphics.lineTo(path[i].x, path[i].y) : graphics.JTopoDashedLineTo(path[i - 1].x, path[i - 1].y, path[i].x, path[i].y, this.dashedPattern);
            if (graphics.stroke(), graphics.closePath(), null != this.arrowsRadius) {
                var lastPrePos = path[path.length - 2],
                    lastPos = path[path.length - 1];
                this.paintArrow(graphics, lastPrePos, lastPos)
            }
        },
        this.paintLoop = function(graphics) {
            graphics.beginPath(); {
                var b = this.bundleGap * (this.nodeIndex + 1) / 2;
                Math.PI + Math.PI / 2
            }
            graphics.arc(this.nodeA.x, this.nodeA.y, b, Math.PI / 2, 2 * Math.PI),
                graphics.stroke(),
                graphics.closePath()
        },
        this.paintArrow = function(graphics, lastPrePosition, lastPos) {
            //根据最后2个点的位置信息 画带箭头的线
            var arrowsOffset = this.arrowsOffset,
                f = this.arrowsRadius / 2,
                PrePos = lastPrePosition,
                pos = lastPos,
                i = Math.atan2(pos.y - PrePos.y, pos.x - PrePos.x),
                j = JTopo.util.getDistance(PrePos, pos) - this.arrowsRadius,
                k = PrePos.x + (j + arrowsOffset) * Math.cos(i),
                l = PrePos.y + (j + arrowsOffset) * Math.sin(i),
                m = pos.x + arrowsOffset * Math.cos(i),
                n = pos.y + arrowsOffset * Math.sin(i);
            i -= Math.PI / 2;
            var o = {
                    x: k + f * Math.cos(i),
                    y: l + f * Math.sin(i)
                },
                p = {
                    x: k + f * Math.cos(i - Math.PI),
                    y: l + f * Math.sin(i - Math.PI)
                };
            graphics.beginPath(),
                graphics.fillStyle = "rgba(" + this.strokeColor + "," + this.alpha + ")",
                graphics.moveTo(o.x, o.y),
                graphics.lineTo(m, n),
                graphics.lineTo(p.x, p.y),
                graphics.stroke(),
                graphics.closePath()
        },
        this.paint = function(graphics) {
            if (null != this.nodeA && null != !this.nodeZ) {
                var path = this.getPath(this.nodeIndex);
                this.path = path,
                    graphics.strokeStyle = "rgba(" + this.strokeColor + "," + this.alpha + ")",
                    graphics.lineWidth = this.lineWidth,
                    this.paintPath(graphics, path),
                path && path.length > 0 && this.paintText(graphics, path)
            }
        };
        var i = -(Math.PI / 2 + Math.PI / 4);
        this.paintText = function(graphics, path) {
            var firstPos = path[0],
                lastPos = path[path.length - 1];
            if (4 == path.length && (firstPos = path[1], lastPos = path[2]), this.text && this.text.length > 0) {
                var x = (lastPos.x + firstPos.x) / 2 + this.textOffsetX,
                    y = (lastPos.y + firstPos.y) / 2 + this.textOffsetY;
                graphics.save(),
                    graphics.beginPath(),
                    graphics.font = this.font;
                var textWidth = graphics.measureText(this.text).width,
                    height = graphics.measureText("田").width;
                if (graphics.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")", this.nodeA === this.nodeZ) {
                    var j = this.bundleGap * (this.nodeIndex + 1) / 2,
                        e = this.nodeA.x + j * Math.cos(i),
                        f = this.nodeA.y + j * Math.sin(i);
                    graphics.fillText(this.text, e, f)
                } else graphics.fillText(this.text, x - textWidth / 2, y - height / 2);
                graphics.stroke(),
                    graphics.closePath(),
                    graphics.restore()
            }
        },
        this.paintSelected = function(graphics) {
            graphics.shadowBlur = 10,
                graphics.shadowColor = "rgba(0,0,0,1)",
                graphics.shadowOffsetX = 0,
                graphics.shadowOffsetY = 0
        },
        this.isInBound = function(b, c) {
            if (this.nodeA === this.nodeZ) {
                var d = this.bundleGap * (this.nodeIndex + 1) / 2,
                    e = JTopo.util.getDistance(this.nodeA, {
                            x: b,
                            y: c
                        }) - d;
                return Math.abs(e) <= 3
            }
            for (var f = !1,
                        g = 1; g < this.path.length; g++) {
                var h = this.path[g - 1],
                    i = this.path[g];
                if (1 == JTopo.util.isPointInLine({
                            x: b,
                            y: c
                        },
                        h, i)) {
                    f = !0;
                    break
                }
            }
            return f
        }
    }
    function FoldLink(nodeA, nodeB, text) {
        this.initialize = function() {
            FoldLink.prototype.initialize.apply(this, arguments),//用link的函数初始化
                this.direction = "horizontal"
        },
        this.initialize(nodeA, nodeB, text),
        this.getStartPosition = function() {
            var position = {
                x: this.nodeA.cx,
                y: this.nodeA.cy
            };
            return "horizontal" == this.direction ? this.nodeZ.cx > position.x ? position.x += this.nodeA.width / 2 : position.x -= this.nodeA.width / 2 : this.nodeZ.cy > position.y ? position.y += this.nodeA.height / 2 : position.y -= this.nodeA.height / 2,
                position
        },
        this.getEndPosition = function() {
            var position = {
                x: this.nodeZ.cx,
                y: this.nodeZ.cy
            };
            return "horizontal" == this.direction ? this.nodeA.cy < position.y ? position.y -= this.nodeZ.height / 2 : position.y += this.nodeZ.height / 2 : position.x = this.nodeA.cx < position.x ? this.nodeZ.x: this.nodeZ.x + this.nodeZ.width,
                position
        },
        this.getPath = function(graphics) {
            var b = [],
                start = this.getStartPosition(),
                end = this.getEndPosition();
            if (this.nodeA === this.nodeZ) return [start, end];
            var f, g, count = getLinksCount(this.nodeA, this.nodeZ),
                i = (count - 1) * this.bundleGap,
                j = this.bundleGap * graphics - i / 2;
            return "horizontal" == this.direction ? (f = end.x + j, g = start.y - j, b.push({
                x: start.x,
                y: g
            }), b.push({
                x: f,
                y: g
            }), b.push({
                x: f,
                y: end.y
            })) : (f = start.x + j, g = end.y - j, b.push({
                x: f,
                y: start.y
            }), b.push({
                x: f,
                y: g
            }), b.push({
                x: end.x,
                y: g
            })),
                b
        },
        this.paintText = function(graphics, path) {
            if (this.text && this.text.length > 0) {
                var c = path[1],
                    d = c.x + this.textOffsetX,
                    e = c.y + this.textOffsetY;
                graphics.save(),
                    graphics.beginPath(),
                    graphics.font = this.font;
                var f = graphics.measureText(this.text).width,
                    g = graphics.measureText("田").width;
                graphics.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")",
                    graphics.fillText(this.text, d - f / 2, e - g / 2),
                    graphics.stroke(),
                    graphics.closePath(),
                    graphics.restore()
            }
        }
    }
    function FlexionalLink(nodeA, nodeB, text) {
        this.initialize = function() {
            FlexionalLink.prototype.initialize.apply(this, arguments),
                this.direction = "vertical",
                this.offsetGap = 44
        },
        this.initialize(nodeA, nodeB, text),
        this.getStartPosition = function() {
            var a = {
                x: this.nodeA.cx,
                y: this.nodeA.cy
            };
            return "horizontal" == this.direction ? a.x = this.nodeZ.cx < a.x ? this.nodeA.x: this.nodeA.x + this.nodeA.width: a.y = this.nodeZ.cy < a.y ? this.nodeA.y: this.nodeA.y + this.nodeA.height,
                a
        },
        this.getEndPosition = function() {
            var a = {
                x: this.nodeZ.cx,
                y: this.nodeZ.cy
            };
            return "horizontal" == this.direction ? a.x = this.nodeA.cx < a.x ? this.nodeZ.x: this.nodeZ.x + this.nodeZ.width: a.y = this.nodeA.cy < a.y ? this.nodeZ.y: this.nodeZ.y + this.nodeZ.height,
                a
        },
        this.getPath = function(nodeIndex) {
            var start = this.getStartPosition(),
                end = this.getEndPosition();
            if (this.nodeA === this.nodeZ) return [start, end];
            var res = [],
                f = getLinksCount(this.nodeA, this.nodeZ),
                g = (f - 1) * this.bundleGap,
                h = this.bundleGap * nodeIndex - g / 2,
                i = this.offsetGap;
            return "horizontal" == this.direction ? (this.nodeA.cx > this.nodeZ.cx && (i = -i), res.push({
                x: start.x,
                y: start.y + h
            }), res.push({
                x: start.x + i,
                y: start.y + h
            }), res.push({
                x: end.x - i,
                y: end.y + h
            }), res.push({
                x: end.x,
                y: end.y + h
            })) : (this.nodeA.cy > this.nodeZ.cy && (i = -i), res.push({
                x: start.x + h,
                y: start.y
            }), res.push({
                x: start.x + h,
                y: start.y + i
            }), res.push({
                x: end.x + h,
                y: end.y - i
            }), res.push({
                x: end.x + h,
                y: end.y
            })),
                res
        }
    }
    function CurveLink(nodeA, nodeB, text) {
        //弧线
        this.initialize = function() {
            CurveLink.prototype.initialize.apply(this, arguments)
        },
        this.initialize(nodeA, nodeB, text),
        this.paintPath = function(graphics, path) {
            if (this.nodeA === this.nodeZ) return void this.paintLoop(graphics);
            graphics.beginPath(),
                graphics.moveTo(path[0].x, path[0].y);
            for (var c = 1; c < path.length; c++) {
                var d = path[c - 1],
                    e = path[c],
                    f = (d.x + e.x) / 2,
                    g = (d.y + e.y) / 2;
                g += (e.y - d.y) / 2,
                    graphics.strokeStyle = "rgba(" + this.strokeColor + "," + this.alpha + ")",
                    graphics.lineWidth = this.lineWidth,
                    graphics.moveTo(d.x, d.cy),
                    graphics.quadraticCurveTo(f, g, e.x, e.y),
                    graphics.stroke()
            }
            if (graphics.stroke(), graphics.closePath(), null != this.arrowsRadius) {
                var h = path[path.length - 2],
                    i = path[path.length - 1];
                this.paintArrow(graphics, h, i)
            }
        }
    }
    Link.prototype = new JTopo.InteractiveElement,
        FoldLink.prototype = new Link,
        FlexionalLink.prototype = new Link,
        CurveLink.prototype = new Link,
        JTopo.Link = Link,
        JTopo.FoldLink = FoldLink,
        JTopo.FlexionalLink = FlexionalLink,
        JTopo.CurveLink = CurveLink
} (JTopo);