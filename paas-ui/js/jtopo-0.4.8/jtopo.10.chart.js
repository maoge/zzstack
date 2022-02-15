!function(JTopo) {
    function PieChartNode() {
        var circleNode = new JTopo.CircleNode;
        return circleNode.radius = 150,
            circleNode.colors = ["#3666B0", "#2CA8E0", "#77D1F6"],
            circleNode.datas = [.3, .3, .4],
            circleNode.titles = ["A", "B", "C"],
            circleNode.paint = function(graphics) {
                var c = 2 * circleNode.radius,
                    d = 2 * circleNode.radius;
                circleNode.width = c,
                    circleNode.height = d;
                for (var e = 0,
                         f = 0; f < this.datas.length; f++) {
                    var g = this.datas[f] * Math.PI * 2;
                    graphics.save(),
                        graphics.beginPath(),
                        graphics.fillStyle = circleNode.colors[f],
                        graphics.moveTo(0, 0),
                        graphics.arc(0, 0, this.radius, e, e + g, !1),
                        graphics.fill(),
                        graphics.closePath(),
                        graphics.restore(),
                        graphics.beginPath(),
                        graphics.font = this.font;
                    var h = this.titles[f] + ": " + (100 * this.datas[f]).toFixed(2) + "%",
                        textWidth = graphics.measureText(h).width,
                        j = (graphics.measureText("田").width, (e + e + g) / 2),
                        k = this.radius * Math.cos(j),
                        l = this.radius * Math.sin(j);
                    j > Math.PI / 2 && j <= Math.PI ? k -= textWidth: j > Math.PI && j < 2 * Math.PI * 3 / 4 ? k -= textWidth: j > 2 * Math.PI * .75,
                        graphics.fillStyle = "#FFFFFF",
                        graphics.fillText(h, k, l),
                        graphics.moveTo(this.radius * Math.cos(j), this.radius * Math.sin(j)),
                    j > Math.PI / 2 && j < 2 * Math.PI * 3 / 4 && (k -= textWidth),
                    j > Math.PI,
                        graphics.fill(),
                        graphics.stroke(),
                        graphics.closePath(),
                        e += g
                }
            },
            circleNode
    }
    function BarChartNode() {
        var node = new JTopo.Node;
        return node.showSelected = !1,
            node.width = 250,
            node.height = 180,
            node.colors = ["#3666B0", "#2CA8E0", "#77D1F6"],
            node.datas = [.3, .3, .4],
            node.titles = ["A", "B", "C"],
            node.paint = function(graphics) {
                var c = 3,
                    d = (this.width - c) / this.datas.length;
                graphics.save(),
                    graphics.beginPath(),
                    graphics.fillStyle = "#FFFFFF",
                    graphics.strokeStyle = "#FFFFFF",
                    graphics.moveTo( - this.width / 2 - 1, -this.height / 2),
                    graphics.lineTo( - this.width / 2 - 1, this.height / 2 + 3),
                    graphics.lineTo(this.width / 2 + c + 1, this.height / 2 + 3),
                    graphics.stroke(),
                    graphics.closePath(),
                    graphics.restore();
                for (var e = 0; e < this.datas.length; e++) {
                    graphics.save(),
                        graphics.beginPath(),
                        graphics.fillStyle = node.colors[e];
                    var f = this.datas[e],
                        g = e * (d + c) - this.width / 2,
                        h = this.height - f - this.height / 2;
                    graphics.fillRect(g, h, d, f);
                    var i = "" + parseInt(this.datas[e]),
                        textWidth = graphics.measureText(i).width,
                        height = graphics.measureText("田").width;
                    graphics.fillStyle = "#FFFFFF",
                        graphics.fillText(i, g + (d - textWidth) / 2, h - height),
                        graphics.fillText(this.titles[e], g + (d - textWidth) / 2, this.height / 2 + height),
                        graphics.fill(),
                        graphics.closePath(),
                        graphics.restore()
                }
            },
            node
    }
    JTopo.BarChartNode = BarChartNode,
        JTopo.PieChartNode = PieChartNode
} (JTopo);