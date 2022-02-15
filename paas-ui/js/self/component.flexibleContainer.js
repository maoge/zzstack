var Component = window.Component || {};


(function(Component) {
	
	/**
	 *  经过封装的container，方便随时调整大小
	 */
	function FlexibleContainer(rows, cols, padding, charaHeight) {
		FlexibleContainer.prototype.initialize.apply(this, null);
		this.rows = rows;
		this.cols = cols;
		this.padding = padding;
		this.charaHeight = charaHeight;
		var layout = JTopo.layout.GridLayout(rows, cols);
		this.layout = layout;
		
		//添加子元素并扩展容器
		this.add = function(element) {
			
			var x = element.x + element.width/2;
			var y = element.y + element.height/2;
			
			if (this.childs.length >= this.rows*this.cols) {
	    		//计算得到是扩行还是扩列
				var xPercent = (x - this.x) / this.width;
				var yPercent = (y - this.y) / this.height;
				xPercent > yPercent ? this.cols++ : this.rows++;
	    		var layout = JTopo.layout.GridLayout(this.rows, this.cols);
	    		this.layout = layout;
	    	}
			
			FlexibleContainer.prototype.add.call(this, element);
	        this.changed = true;
		}
		
		//删除子元素并缩小容器
		this.remove = function(element) {
			FlexibleContainer.prototype.remove.call(this, element);
	        
	        if (this.childs.length <= (this.rows-1)*this.cols && this.rows>1) {
	    		var layout = JTopo.layout.GridLayout(--this.rows, this.cols);
	    		this.layout = layout;
	            this.changed = true;
	        }
		}
		
		//覆盖父类的paint，在绘制前先查看子节点是否有变化，并做尺寸调整
		this.paint = function(graphics) {
			if (this.changed) {
				this.changed = false;
				this.adjustSize();
			}
			FlexibleContainer.prototype.paint.call(this, graphics);
        },
		
		//判断一个坐标点是否在该容器中
		this.isInContainer = function(x, y) {
			if (this.x==null || this.y==null) {
				return false;
			}
			return x>this.x && x<this.x+this.width && y>this.y && y<this.y+this.height;
		}
		
		//根据子元素的最大长宽自动调整容器的长宽
		this.adjustSize = function() {
			var maxW = 0, maxH = 0;
			this.childs.forEach(function (element) {
				if (element.width>maxW) maxW = element.width;
				if (element.textWidth!=undefined && element.textWidth>maxW) maxW = element.textWidth;
				if (element.height>maxH) maxH = element.height;
			});
			
			var width = maxW*this.cols + this.childs[0].width + this.padding*(this.cols-1);
			var height = (maxH+this.charaHeight)*this.rows + this.childs[0].height + this.padding*(this.rows-1);
			var deltaX = width-this.width;
			var deltaY = height-this.height;
			
			this.width = width;
			this.height = height;
			this.x = this.x - deltaX/2;
			this.y = this.y - deltaY/2;
			
			if (this.parentContainer!=undefined && this.parentContainer!=null) {
				this.parentContainer.adjustSize();
			}
		}
		
		//返回一个位置信息的JSON
		this.getPosJson = function() {
			var pos = {};
			pos.x = Math.round(this.x+this.width/2);
			pos.y = Math.round(this.y+this.height/2);
			pos.row = this.rows;
			pos.col = this.cols;
			return pos;
		}
		
		//覆盖原生的paintSelected方法
        this.paintSelected = function(graphics) {
            graphics.shadowBlur = 5,
            graphics.shadowColor = "rgba(128,128,128,1)",
            graphics.shadowOffsetX = 0,
            graphics.shadowOffsetY = 0
        }
	}
	
	FlexibleContainer.prototype = new JTopo.Container;
	Component.FlexibleContainer = FlexibleContainer;

})(Component);
