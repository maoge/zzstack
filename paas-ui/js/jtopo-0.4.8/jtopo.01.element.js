!function(window) {
    //扩展text换行
    CanvasRenderingContext2D.prototype.wrapText = function(str,x,y){
        var textArray = str.split('\n');
        if(textArray==undefined||textArray==null)return false;

        var rowCnt = textArray.length;
        var i = 0,imax = rowCnt,maxLength = 0;maxText = textArray[0];
        for(;i<imax;i++){
            var nowText = textArray[i],textLength = nowText.length;
            if(textLength >=maxLength){
                maxLength = textLength;
                maxText = nowText;
            }
        }
        var maxWidth = this.measureText(maxText).width;
        var lineHeight = this.measureText("元").width;
        x-= lineHeight*2;
        for(var j= 0;j<textArray.length;j++){
            var words = textArray[j];
            this.fillText(words,-(maxWidth/2),y-textArray.length*lineHeight/100);
            y+= lineHeight;
        }
    };

    function Element() {
        this.initialize = function() {
            this.elementType = "element";
            this.serializedProperties = ["elementType"];
            this.propertiesStack = [];
            this._id = JTopo.util.guid();
         };
        this.distroy = function() {};
        this.removeHandler = function() {};
        this.attr = function(property, value) {
            if (null != property && null != value) this[property] = value;
            else if (null != property) return this[property];
            return this;
        };
        this.save = function() {
            var ref = this;
            var props = {};
            this.serializedProperties.forEach(function(prop) {
                props[prop] = ref[prop]
            });
            this.propertiesStack.push(props);
        };
        this.restore = function() {
            if (null != this.propertiesStack && 0 != this.propertiesStack.length) {
                var a = this;
                var b = this.propertiesStack.pop();
                this.serializedProperties.forEach(function(c) {
                    a[c] = b[c]
                })
            }
        };
        this.toJson = function() {
            var a = this;
            var b = "{";
            var c = this.serializedProperties.length;
            return this.serializedProperties.forEach(function(d, e) {
                var f = a[d];
                "string" == typeof f && (f = '"' + f + '"');
                b += '"' + d + '":' + f;
                c > e + 1 && (b += ",");
            });
            b += "}";
        };
    }
    CanvasRenderingContext2D.prototype.JTopoRoundRect = function(a, b, c, d, e) {
        "undefined" == typeof e && (e = 5),
            this.beginPath(),
            this.moveTo(a + e, b),
            this.lineTo(a + c - e, b),
            this.quadraticCurveTo(a + c, b, a + c, b + e),
            this.lineTo(a + c, b + d - e),
            this.quadraticCurveTo(a + c, b + d, a + c - e, b + d),
            this.lineTo(a + e, b + d),
            this.quadraticCurveTo(a, b + d, a, b + d - e),
            this.lineTo(a, b + e),
            this.quadraticCurveTo(a, b, a + e, b),
            this.closePath()
    };
    CanvasRenderingContext2D.prototype.JTopoDashedLineTo = function(a, b, c, d, e) {
            "undefined" == typeof e && (e = 5);
            var f = c - a,
                g = d - b,
                h = Math.floor(Math.sqrt(f * f + g * g)),
                i = 0 >= e ? h: h / e,
                j = g / h * e,
                k = f / h * e;
            this.beginPath();
            for (var l = 0; i > l; l++) l % 2 ? this.lineTo(a + l * k, b + l * j) : this.moveTo(a + l * k, b + l * j);
            this.stroke()
        };
    JTopo = {
        version: "0.4.8",
        zIndex_Container: 1,
        zIndex_Link: 2,
        zIndex_Node: 3,
        SceneMode: {
            normal: "normal",
            drag: "drag",
            edit: "edit",
            select: "select"
        },
        MouseCursor: {
            normal: "default",
            pointer: "pointer",
            top_left: "nw-resize",
            top_center: "n-resize",
            top_right: "ne-resize",
            middle_left: "e-resize",
            middle_right: "e-resize",
            bottom_left: "ne-resize",
            bottom_center: "n-resize",
            bottom_right: "nw-resize",
            move: "move"
            /*
            ,
            open_hand: "url(./img/cur/openhand.cur) 8 8, default",
            closed_hand: "url(./img/cur/closedhand.cur) 8 8, default"
            */
        },
        createStageFromJson: function(jsonStr, canvas) {
            eval("var jsonObj = " + jsonStr);
            var stage = new JTopo.Stage(canvas);
            for (var k in jsonObj)"childs" != k && (stage[k] = jsonObj[k]);
            var scenes = jsonObj.childs;
            return scenes.forEach(function(a) {
                var b = new JTopo.Scene(stage);
                for (var c in a)"childs" != c && (b[c] = a[c]),
                "background" == c && (b.background = a[c]);
                var d = a.childs;
                d.forEach(function(a) {
                    var c = null,
                        d = a.elementType;
                    "node" == d ? c = new JTopo.Node: "CircleNode" == d && (c = new JTopo.CircleNode);
                    for (var e in a) c[e] = a[e];
                    b.add(c)
                })
            }),
                stage
        }
    };
    JTopo.Element = Element;
    window.JTopo = JTopo;
} (window);