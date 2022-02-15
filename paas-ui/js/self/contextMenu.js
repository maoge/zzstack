(function($){
    var ContextMenu = function(option){
        this.DEFAULT = {
            title:null,
            items:[] // {label:'添加', icon:'img/add.png', callback:function() { alert('clicked 1') } },
        };
        this.options = $.extend({},this.DEFAULT,option);
        this.menu = createMenu(this.options);
        this.html = $('html');
        this.show = function(e){
        	$.each($('.contextMenu'),function(){
        		$(this).hide();
        	});
            var that = this,
                left = e.pageX + 5,
                top = e.pageY;
            if (top + this.menu.height() >= $(window).height()) {//如果元素的右键菜单超出最底下范围的话，右键菜单上置
                top -= this.menu.height();
            }
            if (left + this.menu.width() >= $(window).width()) {//如果元素的右键菜单超出最右边范围的话，右键菜单左置
                left -= this.menu.width();
            }
            this.menu.show();
            this.menu.css({zIndex:1000000, left:left, top:top});
            this.html.bind('click',function(){
                that.hide();
                that.html.unbind('click');
                return false;
            });
            return false;
        };
        this.hide = function(e){
            this.menu.hide();
        };
        function createMenu(options){
            var $menu = $('<ul class="contextMenu" style="display: none"><div class="contextMenu"></div></ul>').appendTo(document.body);
            options.title && $('<li class="header"></li>').text(options.title).appendTo($menu);
            options.items.forEach(function(item){
                if(item && item === 'separator'){
                    $('<li class="divider"></li>').appendTo($menu);
                }else{
                    var li = '<li><a href="#">'+
                        (item.icon ? '<img src = "'+item.icon+'" />' : '') +
                        '<span>'+item.label+'</span>' +
                        '</a></li>';
                    var row = $(li).appendTo($menu);

                    if (item.callback) {
                        row.find('a').click(function(){ item.callback(item.label); });
                    }
                }
            });
            return $menu;
        }
    };
    $.fn.contextMenu = function(option){
        var menu = new ContextMenu(option);
        this.unbind('contextmenu',function(){}).bind('contextmenu', function(e) {
            menu.show(e);
            return false;
        });
    }
    $.fn.contextMenu.Constructor = ContextMenu;
    $.extend({//这种调用方式 需要自己去判断条件去show和hide
        contextMenu :function(option){
            var menu = new ContextMenu(option);
            var res = {};
            res.show = function(e){
                menu.show(e);
                menu.menu.find('a').each(function(index,a){
                    $(this).unbind('click').bind('click',function(){
                        option.items[index]['callback'](e,option.items[index]);
                    })
                });
            };
            return res;
        }
    });
})(jQuery);