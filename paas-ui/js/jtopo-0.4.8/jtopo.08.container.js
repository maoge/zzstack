!function(JTopo) {
    function Container(context) {
        this.initialize = function(text) {
            Container.prototype.initialize.apply(this, null),
                this.elementType = "container",
                this.zIndex = JTopo.zIndex_Container,
                this.width = 100,
                this.height = 100,
                this.childs = [],
                this.alpha = .5,
                this.dragable = !0,
                this.childDragble = !0,
                this.visible = !0,
                this.fillColor = "10,100,80",
                this.borderWidth = 0,
                this.borderColor = "255,255,255",
                this.borderRadius = null,
                this.font = "12px Consolas",
                this.fontColor = "255,255,255",
                this.text = text,
                this.textPosition = "Bottom_Center",
                this.textOffsetX = 0,
                this.textOffsetY = 0,
                this.layout = new JTopo.layout.AutoBoundLayout
        },
            this.initialize(context),
            this.add = function(element) {
                this.childs.push(element),
                    element.dragable = this.childDragble;
                element.parentContainer = this;
            },
            this.remove = function(element) {
                for (var i = 0; i < this.childs.length; i++) if (this.childs[i] === element) {
                    element.parentContainer = null,
                        //this.childs = this.childs.del(b),
                        this.childs = JTopo.util.arrayDel(this.childs,i);
                    element.lastParentContainer = this;
                    break;
                }
            },
            this.removeAll = function() {
                this.childs = []
            },
            this.setLocation = function(x, y) {
                var offsetX = x - this.x,
                    offsetY = y - this.y;
                this.x = x,
                    this.y = y;
                for (var i = 0; i < this.childs.length; i++) {
                    var elelment = this.childs[i];
                    elelment.setLocation(elelment.x + offsetX, elelment.y + offsetY)
                }
            },
            this.doLayout = function(layout) {
                layout && layout(this, this.childs)
            },
            this.paint = function(graphics) {
                this.visible && (this.layout && this.layout(this, this.childs),
                    graphics.beginPath(),
                    graphics.fillStyle = "rgba(" + this.fillColor + "," + this.alpha + ")", null == this.borderRadius || 0 == this.borderRadius ? graphics.rect(this.x, this.y, this.width, this.height) : graphics.JTopoRoundRect(this.x, this.y, this.width, this.height, this.borderRadius), graphics.fill(), graphics.closePath(), this.paintText(graphics), this.paintBorder(graphics))
            },
            this.paintBorder = function(graphics) {
                if (0 != this.borderWidth) {
                    graphics.beginPath(),
                        graphics.lineWidth = this.borderWidth,
                        graphics.strokeStyle = "rgba(" + this.borderColor + "," + this.alpha + ")";
                    var b = this.borderWidth / 2;
                    null == this.borderRadius || 0 == this.borderRadius ? graphics.rect(this.x - b, this.y - b, this.width + this.borderWidth, this.height + this.borderWidth) : graphics.JTopoRoundRect(this.x - b, this.y - b, this.width + this.borderWidth, this.height + this.borderWidth, this.borderRadius),
                        graphics.stroke(),
                        graphics.closePath()
                }
            },
            this.paintText = function(graphics) {
                var text = this.text;
                if (null != text && "" != text) {
                    graphics.beginPath(),
                        graphics.font = this.font;
                    var width = graphics.measureText(text).width,
                        distance = graphics.measureText("田").width;
                    //记录text宽度
                    if (!this.textWidth || this.textWidth!=width) {
                        this.textWidth = width;
                        if (this.parentContainer) {
                        	this.parentContainer.changed = true;
                        }
                    }

                    graphics.fillStyle = "rgba(" + this.fontColor + ", " + this.alpha + ")";
                    var position = this.getTextPostion(this.textPosition, width, distance);
                    graphics.fillText(text, position.x, position.y),
                        graphics.closePath()
                }
            },
            this.getTextPostion = function(textPosition, width, distances) {
                var resPosition = null;
                return null == textPosition || "Bottom_Center" == textPosition ? resPosition = {
                    x: this.x + this.width / 2 - width / 2,
                    y: this.y + this.height + distances
                }: "Top_Center" == textPosition ? resPosition = {
                    x: this.x + this.width / 2 - width / 2,
                    y: this.y - distances / 2
                }: "Top_Right" == textPosition ? resPosition = {
                    x: this.x + this.width - width,
                    y: this.y - distances / 2
                }: "Top_Left" == textPosition ? resPosition = {
                    x: this.x,
                    y: this.y - distances / 2
                }: "Bottom_Right" == textPosition ? resPosition = {
                    x: this.x + this.width - width,
                    y: this.y + this.height + distances
                }: "Bottom_Left" == textPosition ? resPosition = {
                    x: this.x,
                    y: this.y + this.height + distances
                }: "Middle_Center" == textPosition ? resPosition = {
                    x: this.x + this.width / 2 - width / 2,
                    y: this.y + this.height / 2 + distances / 2
                }: "Middle_Right" == textPosition ? resPosition = {
                    x: this.x + this.width - width,
                    y: this.y + this.height / 2 + distances / 2
                }: "Middle_Left" == textPosition && (resPosition = {
                    x: this.x,
                    y: this.y + this.height / 2 + distances / 2
                }),
                null != this.textOffsetX && (resPosition.x += this.textOffsetX),
                null != this.textOffsetY && (resPosition.y += this.textOffsetY),
                    resPosition
            },
            this.paintMouseover = function() {},
            this.paintSelected = function(graphics) {
                graphics.shadowBlur = 10,
                    graphics.shadowColor = "rgba(0,0,0,1)",
                    graphics.shadowOffsetX = 0,
                    graphics.shadowOffsetY = 0
            }
    }
    Container.prototype = new JTopo.InteractiveElement,
        JTopo.Container = Container
} (JTopo);