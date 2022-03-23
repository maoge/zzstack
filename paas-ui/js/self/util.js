var Util = window.Util = {
    initLoading : function(){
        if(!this.$backuDrop){
            this.initBackuDrop();
        }
        if(!this.$loading){
            this.$loading = $('<div style="background:url(../images/loading.gif) center center no-repeat #fff;' +
                'width:56px;height:56px;' +
                'position: absolute;' +
                'top:calc(50% - 28px);' +
                'left:calc(50% - 28px);' +
                'line-height:56px;' +
                'font-size:15px;' +
                'opacity: 0.5;' +
                'z-index:2000;' +
                '"></div>').appendTo(this.$backuDrop);
        }
    },
    initBackuDrop : function(){
        if(!this.$backuDrop){
            this.$backuDrop = $('<div class="modal-backdrop fade show"></div>').appendTo(document.body);
        }
    },
    sprintf : function (str) {
        var args = arguments,
            flag = true,
            i = 1;

        str = str.replace(/%s/g, function () {
            var arg = args[i++];

            if (typeof arg === 'undefined') {
                flag = false;
                return '';
            }
            return arg;
        });
        return flag ? str : '';
    },
    msg:function(){
        layer.msg.apply(null,arguments);
    },
    alert:function(type, content) {
        var options = {};

        //这里可以扩展皮肤等
        switch(type) {
            case "error":
                options.icon = 2;
                options.title = "错误";
                break;
            case "warn":
                options.icon = 0;
                options.title = "提示";
                break;
            case "info":
            case "success":
                options.icon = 1;
                options.title = "提示";
                break;
        }

        var index = layer.alert(content, options);
        return index;
    },
    confirm : function(){
        layer.confirm.apply(null,arguments)
    },
    prompt : function () {
        layer.prompt.apply(null, arguments);
    },
    showBackuDrop : function(){
        this.initBackuDrop();
        this.$backuDrop.show();
    },
    hideBackuDrop : function(){
        this.initBackuDrop();
        this.$backuDrop.hide();
    },
    showLoading : function(){
        this.showBackuDrop();
        this.initLoading();
        this.$loading.show();
    },
    hideLoading : function(){
        this.hideBackuDrop();
        this.$loading && this.$loading.hide();
    },
    drag : function(title,body,range){
        var w=window,win=body||title,x,y,_left,_top,range=range||function(x){return x};
        title.style.cursor='move';
        title.onmousedown=function (e){
            e=e||event;
            x=e.clientX,y=e.clientY,_left=win.offsetLeft,_top=win.offsetTop;
            this.ondragstart=function(){return false};
            document.onmousemove=e_move;
            document.onmouseup=undrag
        };
        function e_move(e){
            e=e||event;
            var cl=range(_left+e.clientX-x,'x'),ct=range(_top+e.clientY-y,'y');
            win.style.left=cl+'px';
            win.style.top=ct+'px';
            w.getSelection?w.getSelection().removeAllRanges():
                document.selection.empty();
        };
        function undrag(){this.onmousemove=null};
    },
    strToDate : function(str){
        str = str.replace("/-/g", "/");
        return new Date(str);
    },
    isObjectNotNull : function(obj){
        if(obj && JSON.stringify(obj) != "{}"){
            return true;
        }
        return false;
    },
    getConsumerId : function(len){
        var len = len || 16,
            dict_table = [ '0', '1', '2', '3', '4', '5', '6',
                '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
                'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
                'X', 'Y', 'Z'],
            dict_len = dict_table.length,
            res = "";
        for(var i = 0 ;i<len; i++){
            res += dict_table[Math.floor(Math.random() * dict_len)];
        }
       return "ConId_" + res;
    },
    getFormParam : function(formName){
        //遍历所有的input select textarea元素
        var $form = $("#"+formName),
            params = {};
        $form.find("input,select,textarea").each(function(){
            var $this = $(this),
                type = $this.prop("type"),
                name = $this.prop("name").toUpperCase(),
                val = $this.val();
            if(type === "radio" || type === "checkbox"){
                if($this.prop("checked")){
                    if(params[name]){
                        params[name] = params[name] + "," + val;
                    }else{
                        params[name] = val;
                    }
                }
                return true;//跳出本次循环
            }
            params[name] = val;
        });
        return params;
    },
    clearForm : function(formName){
        var $form = $("#"+formName);
        $form.find("input,select,textarea").each(function(){
            //select默认选择第一个 radio checkbox 不选择 其它的置为空
            var $this = $(this),
                type = $this.prop("type"),
                name = $this.prop("name").toUpperCase();
            //select-multiple很少用不添加，后面需要可以再添加
            if(type === "select-one"){
                $this.find("option:eq(0)").prop("selected",true);
                return true;//跳出本次循环
            }
            if(type === "radio" || type === "checkbox"){
                $this.prop("checked",false);
                return true;//跳出本次循环
            }
            $this.val("");
        });
    },
    setFormData : function(formName,json){
        var $form = $("#"+formName);
        $.each(json,function(name,value){
            var $eles = $form.find("[name='"+name+"']"),
                type = $eles.prop("type");
            if(type == "radio"){
                $eles.each(function(){
                    var $this = $(this);
                    if($this.val() == value){
                        $this.prop("checked",true);
                    }
                })
                return true;
            }
            if(type == "checkbox"){
                var checkValues = value.split(",");
                $eles.each(function(){
                    var $this = $(this);
                    if($.inArray($this.val(),checkValues) > -1){
                        $this.prop("checked",true);
                    }
                })
                return true;
            }
            if(type =="select-one"){
                $eles.find("option[value='"+value+"']").prop("selected",true);
                return true;
            }
            $eles.val(value);
        });
    },
    saveInterval : function (interval) {
        this.intervalArr = this.intervalArr || [];
        this.intervalArr.push(interval);
    },
    clearAllInterval : function () {
        if(this.intervalArr) {
            for(var i in this.intervalArr) {
                clearInterval(this.intervalArr[i]);
            }

            this.intervalArr = [];
        }
    },
    clearInterval : function (interval) {
        if(this.intervalArr) {
            clearInterval(interval);

            var index = this.intervalArr.indexOf(interval);
            if (index > -1) {
                this.intervalArr.splice(index, 1);
            }
            /*
            this.intervalArr.remove(interval);*/
        }

    }
};
/*原生函數扩展*/
//
Date.prototype.simpleFormat = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S": this.getMilliseconds()
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
};

/*Array.prototype.remove = function (val) {
    var index = this.indexOf(val);
    if (index > -1) {
        this.splice(index, 1);
    }
};*/
//全局ajax事件和默认参数设置
$.ajaxSetup({
    type: "post",
    dataType: "json",
    contentType: "application/json; charset=utf-8",
    timeout: 5000,
    beforeSend : function (xhr) {
        xhr.setRequestHeader("MAGIC_KEY", $.cookie("MAGIC_KEY"));
        Util.showLoading();
    },
    error : function (jqxhr) {
        Util.hideLoading();
        if(jqxhr.status === 401 || jqxhr.status === 400){
            /*alert("密码错误或者Token过期，请重新登录！");*/
            window.location.href="../login.html";
            return;
        }
        Util.alert("error", jqxhr.status + ":" + jqxhr.statusText);
    },
    complete : function () {
        Util.hideLoading();
    }
});

function extend(subClass, superClass) {
    var F = function() {};
    F.prototype = superClass.prototype;
    subClass.prototype = new F();
    subClass.prototype.constructor = subClass;

    subClass.superclass = superClass.prototype;
    if(superClass.prototype.constructor == Object.prototype.constructor) {
        superClass.prototype.constructor = superClass;
    }
}

//根据参数名获取url参数
function getUrlParam(name) { 
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i"); 
    var r = window.location.search.substr(1).match(reg); 
    if (r != null) return unescape(r[2]); 
    return null; 
} 