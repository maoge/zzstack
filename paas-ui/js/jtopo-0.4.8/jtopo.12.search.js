!function (JTopo) {
    function analysis(childs, selector) {
        var res = [], matchElements;
        if (0 == childs.length) return res;

        var matched = selector.match(/^\s*(\w+)\s*$/);//任意的空格+中间的word+任意的空格，matchd[1]为去除空格后的匹配结果
        if (null != matched) {
            matchElements = childs.filter(function (element) {
                return element.elementType == matched[1];
            });
            if (null != matchElements && matchElements.length > 0) {
                res = res.concat(matchElements)
            }
            ;
        } else {
            var f = false;
            matched = selector.match(/\s*(\w+)\s*\[\s*(\w+)\s*([>=<])\s*['"](\S+)['"]\s*\]\s*/);//匹配 name[text='ds']文字信息
            if (null == matched || matched.length < 5) {
                matched = selector.match(/\s*(\w+)\s*\[\s*(\w+)\s*([>=<])\s*(\d+(\.\d+)?)\s*\]\s*/);//匹配数字信息node[width>22]
                f = true;
            }
            if (null != matched && matched.length >= 5) {
                var elementType = matched[1],
                    attr = matched[2],
                    operator = matched[3],
                    value = matched[4];
                matchElements = childs.filter(function (ele) {
                    if (ele.elementType != elementType) return !1;
                    var attrValue = ele[attr];
                    f && (attrValue = parseInt(attrValue));
                    return "=" == operator ? attrValue == value : ">" == operator ?
                    attrValue > value : "<" == operator ? value > attrValue : "<=" == operator ? value >= attrValue : ">=" == operator ? attrValue >= value : "!=" == operator ? attrValue != value : false
                });
                null != matchElements && matchElements.length > 0 && (res = res.concat(matchElements))
            }
        }
        return res
    }

    function c(arrayElements) {
        if (arrayElements.find = function (arrayElements) {//往返回的数组对象添加find函数
                return find.call(this, arrayElements)
            },
            eventFun.forEach(function (b) {//添加event函数
                arrayElements[b] = function (a) {
                    for (var c = 0; c < this.length; c++) this[c][b](a);
                    return this
                }
            }),
            arrayElements.length > 0) {
            var b = arrayElements[0];
            for (var c in b) {//添加element的prototype函数
                var f = b[c];
                "function" == typeof f && !
                    function (b) {
                        arrayElements[c] = function () {
                            for (var c = [], d = 0; d < arrayElements.length; d++) c.push(b.apply(arrayElements[d], arguments));
                            return c
                        }
                    }(f)
            }
        }
        //往返回的数组对象添加attr函数，这个函数用于往数组的每一个元素添加额外的属性
        return arrayElements.attr = function (key, value) {
            if (null != key && null != value) for (var i = 0; i < this.length; i++) this[i][key] = value;
            else {
                //不懂这段什么意思
                if (null != key && "string" == typeof key) {
                    for (var d = [], i = 0; i < this.length; i++) d.push(this[i][key]);
                    return d
                }
                if (null != key) for (var c = 0; c < this.length; c++) for (var e in key) this[c][e] = key[e]
            }
            return this
        },
            arrayElements
    }

    function find(param) {
        var currentChilds = [],
            childs = [];

        if (this instanceof JTopo.Stage) {
            currentChilds = this.childs, childs = childs.concat(currentChilds);
        } else if (this instanceof JTopo.Scene) {
            currentChilds = [this];
        } else {
            currentChilds = this, childs = this;//scene.find(fun).find(fun1).find(fun2)
        }

        currentChilds.forEach(function (element) {
            element.childs && (childs = childs.concat(element.childs));//如果是node或者link，就不用遍历childs
        });
        //如果param是fun的话就filter,否则就用选择器按照param规则，比如“node”查找是node节点的数据，"node[text='PD']" 找出node并且text是PD的节点
        var g = "function" == typeof param ? childs.filter(param) : analysis(childs, param);
        return g = c(g);
    }

    var eventFun = "click,mousedown,mouseup,mouseover,mouseout,mousedrag,keydown,keyup".split(",");
    JTopo.Stage.prototype.find = find,
        JTopo.Scene.prototype.find = find
}(JTopo);