!function(JTopo) {
    function getNodesCenter(elements) {//传元素数组，计算元素数组的中心位置
        var cx = 0,
            cy = 0;
        elements.forEach(function(ele) {
            cx += ele.cx, cy += ele.cy;//元素的中心x和中心y相加
        });
        var res = {
            x: cx / elements.length,
            y: cy / elements.length
        };
        return res;
    }
    function circleLayoutNodes(childs, d) {
        null == d && (d = {}); {
            var e = d.cx,
                f = d.cy,
                g = d.minRadius,
                h = d.nodeDiameter,
                i = d.hScale || 1,
                j = d.vScale || 1;
            d.beginAngle || 0,
            d.endAngle || 2 * Math.PI
        }
        if (null == e || null == f) {
            var k = getNodesCenter(childs);
            e = k.x,
                f = k.y
        }
        var l = 0,
            m = [],
            n = [];
        childs.forEach(function(a) {
            null == d.nodeDiameter ? (a.diameter && (h = a.diameter), h = a.radius ? 2 * a.radius: Math.sqrt(2 * a.width * a.height), n.push(h)) : n.push(h),
                l += h
        }),
            childs.forEach(function(a, b) {
                var c = n[b] / l;
                m.push(Math.PI * c)
            });
        var o = (childs.length, m[0] + m[1]),
            p = n[0] / 2 + n[1] / 2,
            q = p / 2 / Math.sin(o / 2);
        null != g && g > q && (q = g);
        var r = q * i,
            s = q * j,
            t = d.animate;
        if (t) {
            var u = t.time || 1e3,
                v = 0;
            childs.forEach(function(b, c) {
                v += 0 == c ? m[c] : m[c - 1] + m[c];
                var d = e + Math.cos(v) * r,
                    g = f + Math.sin(v) * s;
                JTopo.Animate.stepByStep(b, {
                        x: d - b.width / 2,
                        y: g - b.height / 2
                    },
                    u).start()
            })
        } else {
            var v = 0;
            childs.forEach(function(a, b) {
                v += 0 == b ? m[b] : m[b - 1] + m[b];
                var c = e + Math.cos(v) * r,
                    d = f + Math.sin(v) * s;
                a.cx = c,
                    a.cy = d
            })
        }
        return {
            cx: e,
            cy: f,
            radius: r,
            radiusA: r,
            radiusB: s
        }
    }
    function GridLayout(rows, cols) {
        //返回一个函数，用于container.paint的时候调用，设置nodes的位置
        return function(container) {
            var nodeChilds = container.childs;
            if (! (nodeChilds.length <= 0)) {
                for (var bound = container.getBound(),
                         firstNode = nodeChilds[0],
                         g = (bound.width - firstNode.width) / cols,
                         h = (bound.height - firstNode.height) / rows,
                         i = (nodeChilds.length, 0),
                         j = 0; rows > j; j++){
                    for (var k = 0; cols > k; k++) {
                        var node = nodeChilds[i++],
                            m = bound.left + g / 2 + k * g,
                            n = bound.top + h / 2 + j * h;
                        if (node.setLocation(m, n), i >= nodeChilds.length) return
                    }
                }
            }
        }
    }
    function FlowLayout(horizontal, vertical) {
        //流式布局，传入2个node之间的水平、垂直间隔
        return null == horizontal && (horizontal = 0),
        null == vertical && (vertical = 0),
            function(container) {
                var nodeChilds = container.childs;
                if (! (nodeChilds.length <= 0)){
                    for (var bounds = container.getBound(), left = bounds.left, top = bounds.top, h = 0; h < nodeChilds.length; h++) {
                        var node = nodeChilds[h];
                        left + node.width >= bounds.right && (left = bounds.left, top += vertical + node.height),
                            node.setLocation(left, top),
                            left += horizontal + node.width
                    }
                }
            }
    }
    function AutoBoundLayout() {
        //默认的布局，自动扩展
        return function(container, nodeChilds) {
            if (nodeChilds.length > 0) {
                for (var c = 1e7,
                         d = -1e7,
                         e = 1e7,
                         f = -1e7,
                         g = d - c,
                         h = f - e,
                         i = 0; i < nodeChilds.length; i++) {
                    var node = nodeChilds[i];
                    node.x <= c && (c = node.x),
                    node.x >= d && (d = node.x),
                    node.y <= e && (e = node.y),
                    node.y >= f && (f = node.y),
                        g = d - c + node.width,
                        h = f - e + node.height
                }
                container.x = c,
                    container.y = e,
                    container.width = g,
                    container.height = h
            }
        }
    }
    function getRootNodes(childs) {
        //获取treeLayout的根节点，传进来container里面的所有childs
        var nodes = [],
            links = childs.filter(function(b) {
                return b instanceof JTopo.Link ? !0 : (nodes.push(b), !1)
            });
        return childs = nodes.filter(function(node) {
            for (var b = 0; b < links.length; b++) if (links[b].nodeZ === node) return ! 1;
            return ! 0
        }),
            childs = childs.filter(function(node) {
                for (var b = 0; b < links.length; b++) if (links[b].nodeA === node) return ! 0;
                return ! 1
            })
    }
    function getAutoWidthHeight(nodeChilds) {
        //计算treeLayout的时候，不赋值widhth和levelheight的时候，根据所有节点计算合适的width和height不建议用这个
        var b = 0,
            c = 0;
        return nodeChilds.forEach(function(node) {
            b += node.width,
                c += node.height
        }),
        {
            width: b / nodeChilds.length,
            height: c / nodeChilds.length
        }
    }
    function updateParentNodeLocation(childs, parentNode, width, height) {//childs, rootNode
        parentNode.x += width,
            parentNode.y += height;
        for (var nodeChilds = getNodeChilds(childs, parentNode), f = 0; f < nodeChilds.length; f++) updateParentNodeLocation(childs, nodeChilds[f], width, height)
    }
    function getAllTreeNodes(childs, rootNode) {
        //获得所有的树节点数组
        function getTreeNode(parentNode, deep) {
            var nodeChilds = getNodeChilds(childs, parentNode);
            null == nodes[deep] && (nodes[deep] = {},
                nodes[deep].nodes = [], nodes[deep].childs = []),
                nodes[deep].nodes.push(parentNode),
                nodes[deep].childs.push(nodeChilds);
            for (var i = 0; i < nodeChilds.length; i++) getTreeNode(nodeChilds[i], deep + 1),
                nodeChilds[i].parent = parentNode
        }
        var nodes = [];
        return getTreeNode(rootNode, 0),
            nodes
    }
    function TreeLayout(direction, width, levelHeight) {
        return function(container) {
            function f(childs, rootNode) {
                for (var deep = JTopo.layout.getTreeDeep(childs, rootNode), treeNodes = getAllTreeNodes(childs, rootNode), leafLevelNodes = treeNodes["" + deep].nodes, i = 0; i < leafLevelNodes.length; i++) {
                    var leafNode = leafLevelNodes[i],
                        x = (i + 1) * (width + 10),
                        y = deep * levelHeight;
                    "down" == direction || ("up" == direction ? y = -y: "left" == direction ? (x = -deep * levelHeight, y = (i + 1) * (width + 10)) : "right" == direction && (x = deep * levelHeight, y = (i + 1) * (width + 10))),

                        leafNode.setLocation(x, y)
                }
                for (var q = deep - 1; q >= 0; q--) for (var parentNodes = treeNodes["" + q].nodes, levelChildsNodes = treeNodes["" + q].childs, i = 0; i < parentNodes.length; i++) {
                    var parentNode = parentNodes[i],
                        parentNodeChilds = levelChildsNodes[i];
                    if ("down" == direction ? parentNode.y = q * levelHeight: "up" == direction ? parentNode.y = -q * levelHeight: "left" == direction ? parentNode.x = -q * levelHeight: "right" == direction && (parentNode.x = q * levelHeight), parentNodeChilds.length > 0 ? "down" == direction || "up" == direction ? parentNode.x = (parentNodeChilds[0].x + parentNodeChilds[parentNodeChilds.length - 1].x) / 2 : ("left" == direction || "right" == direction) && (parentNode.y = (parentNodeChilds[0].y + parentNodeChilds[parentNodeChilds.length - 1].y) / 2) : i > 0 && ("down" == direction || "up" == direction ? parentNode.x = parentNodes[i - 1].x + parentNodes[i - 1].width + width: ("left" == direction || "right" == direction) && (parentNode.y = parentNodes[i - 1].y + parentNodes[i - 1].height + width)), i > 0) if ("down" == direction || "up" == direction) {
                        if (parentNode.x < parentNodes[i - 1].x + parentNodes[i - 1].width) for (var v = parentNodes[i - 1].x + parentNodes[i - 1].width + width, w = Math.abs(v - parentNode.x), x = i; x < parentNodes.length; x++) updateParentNodeLocation(container.childs, parentNodes[x], w, 0)
                    } else if (("left" == direction || "right" == direction) && parentNode.y < parentNodes[i - 1].y + parentNodes[i - 1].height) for (var y = parentNodes[i - 1].y + parentNodes[i - 1].height + width, z = Math.abs(y - parentNode.y), x = i; x < parentNodes.length; x++) updateParentNodeLocation(container.childs, parentNodes[x], 0, z)
                }
            }
            var autoWidthHeight = null;
            null == width && (autoWidthHeight = getAutoWidthHeight(container.childs), width = autoWidthHeight.width, ("left" == direction || "right" == direction) && (width = autoWidthHeight.width + 10)),
            null == levelHeight && (null == autoWidthHeight && (autoWidthHeight = getAutoWidthHeight(container.childs)), levelHeight = 2 * autoWidthHeight.height),
            null == direction && (direction = "down");
            var rootNodes = JTopo.layout.getRootNodes(container.childs);
            if (rootNodes.length > 0) {
                f(container.childs, rootNodes[0]);
                var bound = JTopo.util.getElementsBound(container.childs),
                    m = container.getCenterLocation(),
                    n = m.x - (bound.left + bound.right) / 2,
                    o = m.y - (bound.top + bound.bottom) / 2;
                container.childs.forEach(function(b) {
                    b instanceof JTopo.Node && (b.x += n, b.y += o)
                })
            }
        }
    }
    function CircleLayout(circleRadius) {
        return function(container) {
            function setLocation(childs, rootNode, currRadius) {
                var nodeChilds = getNodeChilds(childs, rootNode);
                if (0 != nodeChilds.length) {
                    null == currRadius && (currRadius = circleRadius);
                    var g = 2 * Math.PI / nodeChilds.length;
                    nodeChilds.forEach(function(node, index) {
                        var x = rootNode.x + currRadius * Math.cos(g * index),
                            y = rootNode.y + currRadius * Math.sin(g * index);
                        node.setLocation(x, y);
                        var j = currRadius / 2;
                        setLocation(childs, node, j)
                    })
                }
            }
            var rootNodes = JTopo.layout.getRootNodes(container.childs);
            if (rootNodes.length > 0) {
                setLocation(container.childs, rootNodes[0]);
                var bound = JTopo.util.getElementsBound(container.childs),
                    centerLocations = container.getCenterLocation(),
                    h = centerLocations.x - (bound.left + bound.right) / 2,
                    i = centerLocations.y - (bound.top + bound.bottom) / 2;
                container.childs.forEach(function(ele) {
                    ele instanceof JTopo.Node && (ele.x += h, ele.y += i)
                })
            }
        }
    }
    /*function m(a, b, c, d, e, f) {
     for (var g = [], h = 0; c > h; h++) for (var i = 0; d > i; i++) g.push({
     x: a + i * e,
     y: b + h * f
     });
     return g
     }*/
    function circleLayoutPosCal(cx, cy, nodesNum, radius, beginAngle, endAngle) {
        beginAngle = beginAngle || 0,
            endAngle = endAngle || 2 * Math.PI;
        var angle = endAngle - beginAngle,
            angleInterval = angle / nodesNum,
            res = [];
        beginAngle += angleInterval / 2;
        for (var l = beginAngle; endAngle >= l; l += angleInterval) {
            var m = cx + Math.cos(l) * radius,
                n = cy + Math.sin(l) * radius;
            res.push({
                x: m,
                y: n
            })
        }
        return res
    }
    function treeLayoutPosCal(cx, cy, nodesNum, width, heigth, direction) {
        direction = direction || "bottom";
        var res = [];
        if ("bottom" == direction) for (var i = cx - nodesNum / 2 * width + width / 2,
                                            j = 0; nodesNum >= j; j++) res.push({
            x: i + j * width,
            y: cy + heigth
        });
        else if ("top" == direction) for (var i = cx - nodesNum / 2 * width + width / 2,
                                              j = 0; c >= j; j++) res.push({
            x: i + j * width,
            y: cy - heigth
        });
        else if ("right" == direction) for (var i = cy - nodesNum / 2 * width + width / 2,
                                                j = 0; nodesNum >= j; j++) res.push({
            x: cx + heigth,
            y: i + j * width
        });
        else if ("left" == direction) for (var i = cy - nodesNum / 2 * width + width / 2,
                                               j = 0; nodesNum >= j; j++) res.push({
            x: cx - heigth,
            y: i + j * width
        });
        return res
    }
    function gridLayoutPosCal(cx, cy, rows, cols, horizontal, vertical) {
        //这个样式很丑，可换个排序的样子
        for (var g = [], h = 0; rows > h; h++) for (var i = 0; cols > i; i++) g.push({
            x: cx + i * horizontal,
            y: cy + h * vertical
        });
        return g
    }
    function adjustPosition(rootNode, nodeChilds) {
        //调整布局里面的节点位置
        if (rootNode.layout) {
            var layout = rootNode.layout,
                type = layout.type,
                nodeChildsPositions = null;
            if ("circle" == type) {
                var radius = layout.radius || Math.max(rootNode.width, rootNode.height);
                nodeChildsPositions = circleLayoutPosCal(rootNode.cx, rootNode.cy, nodeChilds.length, radius, layout.beginAngle, layout.endAngle);
            } else if ("tree" == type) {
                var width = layout.width || 50,
                    height = layout.height || 50,
                    direction = layout.direction;
                nodeChildsPositions = treeLayoutPosCal(rootNode.cx, rootNode.cy, nodeChilds.length, width, height, direction);
            } else {
                if ("grid" != type) return;
                nodeChildsPositions = gridLayoutPosCal(rootNode.x, rootNode.y, layout.rows, layout.cols, layout.horizontal || 0, layout.vertical || 0)
            }
            for (var j = 0; j < nodeChilds.length; j++) nodeChilds[j].setCenterLocation(nodeChildsPositions[j].x, nodeChildsPositions[j].y)
        }
    }
    function getNodeChilds(childs, rootNode) {
        //根据节点查询所有该节点连线的其它节点nodeA -》 nodeZ
        for (var d = [], i = 0; i < childs.length; i++)
            childs[i] instanceof JTopo.Link && childs[i].nodeA === rootNode && d.push(childs[i].nodeZ);
        return d
    }
    function layoutNode(sence, rootNode, isRecursion) {
        //对单个节点进行布局 JTopo.layout.layoutNode(scene, currentNode, true);
        var nodeChilds = getNodeChilds(sence.childs, rootNode);
        if (0 == nodeChilds.length) return null;
        if (adjustPosition(rootNode, nodeChilds), 1 == isRecursion) for (var e = 0; e < nodeChilds.length; e++) layoutNode(sence, nodeChilds[e], isRecursion);
        return null;
    }
    function springLayout(b, c) {
        function d(a, b) {
            var c = a.x - b.x,
                d = a.y - b.y;
            i += c * f,
                j += d * f,
                i *= g,
                j *= g,
                j += h,
                b.x += i,
                b.y += j
        }
        function e() {
            if (! (++k > 150)) {
                for (var a = 0; a < l.length; a++) l[a] != b && d(b, l[a], l);
                setTimeout(e, 1e3 / 24)
            }
        }
        var f = .01,
            g = .95,
            h = -5,
            i = 0,
            j = 0,
            k = 0,
            l = c.getElementsByClass(a.Node);
        e()
    }
    function getTreeDeep(childs, rootNode) {
        //获得treeLayout的tree深度
        function calDeep(childs, rootNode, currDeep) {
            var nodeChilds = getNodeChilds(childs, rootNode);
            currDeep > deep && (deep = currDeep);
            for (var g = 0; g < nodeChilds.length; g++) calDeep(childs, nodeChilds[g], currDeep + 1)
        }
        var deep = 0;
        return calDeep(childs, rootNode, 0),
            deep
    }
    JTopo.layout = {
        layoutNode: layoutNode,
        getNodeChilds: getNodeChilds,
        adjustPosition: adjustPosition,
        springLayout: springLayout,
        getTreeDeep: getTreeDeep,
        getRootNodes: getRootNodes,
        GridLayout: GridLayout,
        FlowLayout: FlowLayout,
        AutoBoundLayout: AutoBoundLayout,
        CircleLayout: CircleLayout,
        TreeLayout: TreeLayout,
        getNodesCenter: getNodesCenter,
        circleLayoutNodes: circleLayoutNodes
    }
} (JTopo);