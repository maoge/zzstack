!function(JTopo) {
    function AnimateTool(invokeFun, interval) {
        //动画的启动和停止函数
        var animateInterval, messageBus = null;
        return {
            stop: function() {
                return animateInterval ? (window.clearInterval(animateInterval), messageBus && messageBus.publish("stop"), this) : this
            },
            start: function() {
                var self = this;
                return animateInterval = setInterval(function() {
                        invokeFun.call(self)
                    },
                    interval),
                    this
            },
            onStop: function(callback) {
                return null == messageBus && (messageBus = new JTopo.util.MessageBus),
                    messageBus.subscribe("stop", callback),
                    this
            }
        }
    }
    function effectGravity(node, options) {
        options = options || {};
        var gravity = options.gravity || .1,
            dx = options.dx || 0,
            dy = options.dy || 5,
            stop = options.stop,
            interval = options.interval || 30,
            animateTool = new AnimateTool(function() {
                    stop && stop() ? (dy = .5, this.stop()) : (dy += gravity, node.setLocation(node.x + dx, node.y + dy))
                },
                interval);
        return animateTool
    }
    function stepByStep(element, animates, interval, needLoop, f) {
        var frameInterval = 1000 / 24,//1秒24帧 
            wholeAnimates = {};
        for (var index in animates) {
            var action = animates[index],
                k = action - element[index];
            wholeAnimates[index] = {
                oldValue: element[index],
                targetValue: action,
                step: k / interval * frameInterval,
                isDone: function(b) {
                    var c = this.step > 0 && element[b] >= this.targetValue || this.step < 0 && element[b] <= this.targetValue;
                    return c
                }
            }
        }
        var animateTool = new AnimateTool(function() {
                var b = !0;
                for (var index in animates) wholeAnimates[index].isDone(index) || (element[index] += wholeAnimates[index].step, b = !1);
                if (b) {
                    if (!needLoop) return this.stop();
                    for (var i in animates) if (f) {
                        var g = wholeAnimates[i].targetValue;
                        wholeAnimates[i].targetValue = wholeAnimates[i].oldValue,
                            wholeAnimates[i].oldValue = g,
                            wholeAnimates[i].step = -wholeAnimates[i].step
                    } else element[i] = wholeAnimates[i].oldValue
                }
                return this
            },
            frameInterval);
        return animateTool;
    }
    function spring(options) {//弹性var effect = JTopo.Effect.spring({grivity: 10 // 引力 (可以为负值)})
        null == options && (options = {});
        var b = options.spring || .1,
            friction = options.friction || .8,//摩擦系数
            grivity = options.grivity || 0,//引力
            minLength = (options.wind || 0, options.minLength || 0);
        return {
            items: [],
            timer: null,
            isPause: !1,
            addNode: function(node, target) {
                var item = {
                    node: node,
                    target: target,
                    vx: 0,
                    vy: 0
                };
                return this.items.push(item),
                    this
            },
            play: function(frameInterval) {
                this.stop(),
                    frameInterval = null == frameInterval ? 1000 / 24 : frameInterval;
                var self = this;
                this.timer = setInterval(function() {
                        self.nextFrame();
                    },
                    frameInterval)
            },
            stop: function() {
                null != this.timer && window.clearInterval(this.timer)
            },
            nextFrame: function() {
                for (var a = 0; a < this.items.length; a++) {
                    var item = this.items[a],
                        node = item.node,
                        target = item.target,
                        vx = item.vx,
                        vy = item.vy,
                        k = target.x - node.x,
                        l = target.y - node.y,
                        m = Math.atan2(l, k);
                    if (0 != minLength) {
                        var n = target.x - Math.cos(m) * minLength,
                            o = target.y - Math.sin(m) * minLength;
                        vx += (n - node.x) * b,
                            vy += (o - node.y) * b
                    } else vx += k * b,
                        vy += l * b;
                    vx *= friction,
                        vy *= friction,
                        vy += grivity,
                        node.x += vx,
                        node.y += vy,
                        item.vx = vx,
                        item.vy = vy
                }
            }
        }
    }
    function rotate(node, options) {//旋转 JTopo.Animate.rotate(node, {context:scene}).run();
        function run() {
            return rotateInterval = setInterval(function() {
                    return stopGlobalLoop ? void self.stop() : (node.rotate += g || .2, void(node.rotate > 2 * Math.PI && (node.rotate = 0)))
                },
                100),
                self
        }
        function stop() {
            return window.clearInterval(rotateInterval),
            self.onStop && self.onStop(node),
                self
        }
        var rotateInterval = (options.context, null),
            self = {},
            g = options.v;
        return self.run = run,
            self.stop = stop,
            self.onStop = function(callback) {
                return self.onStop = callback,
                    self
            },
            self
    }
    function gravity(node, options) {
        function clearInterval() {
            return window.clearInterval(gravityInterval),
            self.onStop && self.onStop(node),
                self
        }
        function d() {
            var d = options.dx || 0,
                i = options.dy || 2;
            return gravityInterval = setInterval(function() {
                    return stopGlobalLoop ? void self.stop() : (i += f, void(node.y + node.height < e.stage.canvas.height ? node.setLocation(node.x + d, node.y + i) : (i = 0, clearInterval())))
                },
                20),
                self
        }
        var e = options.context,
            gravity = options.gravity || .1,
            gravityInterval = null,
            self = {};
        return self.run = d,
            self.stop = clearInterval,
            self.onStop = function(callback) {
                return self.onStop = callback,
                    self
            },
            self
    }
    function dividedTwoPiece(node, options) {//切成两半
        /**JTopo.Animate.dividedTwoPiece(node, {angle: angle, context:scene}).run().onStop(function(n){
                            var fruit = randomFruit();
                            node.setImage('./img/fruit/'+fruit+'.png', true);
                        });*/
        function drawArc(x, y, radius, startAngle, endAngle) {
            var node = new JTopo.Node;
            return node.setImage(node.image),
                node.setSize(node.width, node.height),
                node.setLocation(x, y),
                node.showSelected = !1,
                node.dragable = !1,
                node.paint = function(graphics) {
                    graphics.save(),
                        graphics.arc(0, 0, radius, startAngle, endAngle),
                        graphics.clip(),
                        graphics.beginPath(),
                        null != this.image ? graphics.drawImage(this.image, -this.width / 2, -this.height / 2) : (graphics.fillStyle = "rgba(" + this.style.fillStyle + "," + this.alpha + ")", graphics.rect( - this.width / 2, -this.height / 2, this.width / 2, this.height / 2), graphics.fill()),
                        graphics.closePath(),
                        graphics.restore()
                },
                node
        }
        function dividedTwoPieceAnimate(c, e) {
            var f = c,
                g = c + Math.PI,
                h = drawArc(node.x, node.y, node.width, f, g),
                j = drawArc(node.x - 2 + 4 * Math.random(), node.y, node.width, f + Math.PI, f);
            node.visible = !1,
                e.add(h),
                e.add(j),
                JTopo.Animate.gravity(h, {
                    context: e,
                    dx: .3
                }).run().onStop(function() {
                    e.remove(h),
                        e.remove(j),
                        self.stop()
                }),
                JTopo.Animate.gravity(j, {
                    context: e,
                    dx: -.2
                }).run()
        }
        function run() {
            return dividedTwoPieceAnimate(options.angle, h),
                self
        }
        function stop() {
            return self.onStop && self.onStop(node),
                self
        }
        var h = options.context,
            self = (node.style, {});
        return self.onStop = function(callback) {
            return self.onStop = callback,
                self
        },
            self.run = run,
            self.stop = stop,
            self
    }
    function repeatThrow(node, options) {//不停的抛 JTopo.Animate.repeatThrow(node, {context:scene}).run()
        function repeatThrowAnimate(nodeEle) {
            nodeEle.visible = !0,
                nodeEle.rotate = Math.random();
            var b = context.stage.canvas.width / 2;
            nodeEle.x = b + Math.random() * (b - 100) - Math.random() * (b - 100),
                nodeEle.y = context.stage.canvas.height,
                nodeEle.vx = 5 * Math.random() - 5 * Math.random(),
                nodeEle.vy = -25
        }
        function run() {
            return repeatThrowAnimate(node),
                repeatThrowInterval = setInterval(function() {
                        return stopGlobalLoop ? void self.stop() : (node.vy += f, node.x += node.vx, node.y += node.vy, void((node.x < 0 || node.x > context.stage.canvas.width || node.y > context.stage.canvas.height) && (self.onStop && self.onStop(node), repeatThrowAnimate(node))))
                    },
                    50),
                self
        }
        function clearInterval() {
            window.clearInterval(repeatThrowInterval)
        }
        var f = .8,
            context = options.context,
            repeatThrowInterval = null,
            self = {};
        return self.onStop = function(callback) {
            return self.onStop = callback,
                self
        },
            self.run = run,
            self.stop = clearInterval,
            self
    }
    function stopAll() {
        stopGlobalLoop = !0
    }
    function startAll() {
        stopGlobalLoop = !1
    }
    function cycle(node, options) {//晃动 JTopo.Animate.cycle(newNode, {context:scene, p1:{x:34, y:190}, p2:{x:34, y:200}}).run();
        function run() {
            return cycleInterval = setInterval(function() {
                    if (stopGlobalLoop) return void self.stop();
                    var a = f.y + h + Math.sin(k) * j;
                    node.setLocation(node.x, a),
                        k += speed
                },
                100),
                self
        }
        function clearInterval() {
            window.clearInterval(cycleInterval)
        }
        var f = options.p1,
            g = options.p2,
            h = (options.context, f.x + (g.x - f.x) / 2),
            i = f.y + (g.y - f.y) / 2,
            j = JTopo.util.getDistance(f, g) / 2,
            k = Math.atan2(i, h),
            speed = options.speed || .2,
            self = {},
            cycleInterval = null;
        return self.run = run,
            self.stop = clearInterval,
            self
    }
    function move(node, options) {//JTopo.Animate.move(overNode, {context:scene, position:{x:60, y:200}}).run();
        function run() {
            return moveInterval = setInterval(function() {
                    if (stopGlobalLoop) return void self.stop();
                    var b = position.x - node.x,
                        c = position.y - node.y,
                        h = b * easing,
                        i = c * easing;
                    node.x += h,
                        node.y += i,
                    .01 > h && .1 > i && clearInterval()
                },
                100),
                self
        }
        function clearInterval() {
            window.clearInterval(moveInterval)
        }
        var position = options.position,
            easing = (options.context, options.easing || .2),
            self = {},
            moveInterval = null;
        return self.onStop = function(a) {
            return self.onStop = a,
                self
        },
            self.run = run,
            self.stop = clearInterval,
            self
    }
    function scale(node, options) {//缩放变换 JTopo.Animate.scale(node, {scale: 2, context:scene}).run().onStop(function(n){});
        function run() {
            return scaleInterval = setInterval(function() {
                    node.scaleX += f,
                        node.scaleY += f,
                    node.scaleX >= scale && stop()
                },
                100),
                self
        }
        function stop() {
            self.onStop && self.onStop(node),
                node.scaleX = scaleX,
                node.scaleY = scaleY,
                window.clearInterval(scaleInterval)
        }
        var scale = (options.position, options.context, options.scale || 1),
            f = .06,
            scaleX = node.scaleX,
            scaleY = node.scaleY,
            self = {},
            scaleInterval = null;
        return self.onStop = function(callback) {
            return self.onStop = callback,
                self
        },
            self.run = run,
            self.stop = stop,
            self
    }
    JTopo.Animate = {},
        JTopo.Effect = {};
    var stopGlobalLoop = !1;
    JTopo.Effect.spring = spring,
        JTopo.Effect.gravity = effectGravity,
        JTopo.Animate.stepByStep = stepByStep,
        JTopo.Animate.rotate = rotate,
        JTopo.Animate.scale = scale,
        JTopo.Animate.move = move,
        JTopo.Animate.cycle = cycle,
        JTopo.Animate.repeatThrow = repeatThrow,
        JTopo.Animate.dividedTwoPiece = dividedTwoPiece,
        JTopo.Animate.gravity = gravity,
        JTopo.Animate.startAll = startAll,
        JTopo.Animate.stopAll = stopAll
} (JTopo);