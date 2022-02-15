var Component = window.Component || {};


(function(Component) {
	
	/**
	 *  经过封装的node，在图上有各种标志代表实例的各种状态
	 */
	function StatusNode() {
		StatusNode.prototype.initialize.apply(this, null);
		this.beginFlash = 1;
		this.graduallyAdd = true ;

		this.paintImage = function(canvas) {
			var alpha = canvas.globalAlpha;
			canvas.globalAlpha = this.alpha;

			canvas.drawImage(this.image, -this.width / 2, -this.height / 2, this.width, this.height);

//			canvas.globalAlpha = 0.9;
            
            // "-1"  -  "new"
            //  "0"  -  "saved"
            //  "1"  -  "deployed"
            //  "2"  -  "warn"
            //  "3"  -  "error"
            //  "4"  -  "alarm"
            //  "5"  -  "preEmbadded"

			if (this.status == "0") {
				canvas.drawImage(this.statusIcons.saved, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
            } else if (this.status == "1") {
                canvas.drawImage(this.statusIcons.deployed, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
            } else if (this.status == "2") {
                canvas.drawImage(this.statusIcons.warn, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
            } else if (this.status == "3") {
                canvas.drawImage(this.statusIcons.error, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
            } else if (this.status == "4") {
				this.flash(canvas);
				canvas.drawImage(this.statusIcons.error, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
			} else if (this.status == "5") {
                canvas.drawImage(this.statusIcons.preEmbadded, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
            }
			if(this.pitchStatus == "master1" || this.pitchStatus == "backup0"){
				canvas.drawImage(this.pitchIcon, this.width/12, this.height/12, this.width/12*5, this.height/12*5);
			}
			canvas.globalAlpha = alpha;
		};
		//闪烁的方法
		this.flash = function(canvas){
			if(this.beginFlash == 20){
				this.graduallyAdd = false;
			}
			if(this.beginFlash == 1){
				this.graduallyAdd = true;
			}
			canvas.globalAlpha = ((this.graduallyAdd ? this.beginFlash ++ : this.beginFlash --)  % 20) / 20;
		};
	}
	
	StatusNode.prototype = new JTopo.Node;
	Component.StatusNode = StatusNode;

})(Component);
