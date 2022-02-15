(function ($) {
    
    var sprintf = function (str) {
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
    };
    var MTable = function(ele,options){
        this.options = options;
        this.$ele = $(ele);
        this.init();
    };
    MTable.DEFAULTS={
        striped: false,//斑马纹
        url: undefined,//ajax请求路径
        countUrl:undefined,//如果有分页，并且是ajax方式。一定要有这个{count:12}
        queryParams:{},//附加的查询参数
        contentType: 'application/json',
        dataType: 'json',
        ajaxType:'post',//get post
        ajaxCache:false,//true false
        pagination: false,//是否分页
        pageNumber: 1,//分页的页码
        pageSize: 20,//每一页的数量
        //pageList: [10, 25, 50, 100],//可选的每一页数量
        showHeader: true,//是否显现表头
        columns: [[]],
        data: [],//如果有url就不用data，如果不想用url，可以自己请求ajax后，把值给data，这种方式不支持分页，需要自己逻辑处理
        height: undefined,//高度
        undefinedText: '-',//没有值显示的时候，默认的文本
        idField: undefined,// data中存储复选框value的键值
        checkboxHeader: true,// 为否隐藏全选按钮
        singleSelect:false,//是否只能单选
        paginationPreText: '&lsaquo;',//上一页的字 默认为<
        paginationNextText: '&rsaquo;',//上一页的字 默认为>
        retCode:"RET_CODE",//用于解析
        retInfo:"RET_INFO",
        countCloumnName:"COUNT",
        countAjaxError:function(){},
        dataAjaxError:function(){},
        ajaxError:function(){}
    };
    MTable.COLUMN_DEFAULTS={
        checkbox: false,
        field:undefined,//data或者url里面的字段名称，如果是checkbox，buttons可以忽略
        titleTooltip: undefined,
        title: undefined,
        align: 'center', // left, right, center
        valign: 'middle', // top, middle, bottom
        width: undefined,
        visible: true,//是否显示，可以作为隐藏列，false为隐藏
        cellStyle: undefined,
        isButtonColumn:false,
        buttons:[],//｛text:'按钮名称',name:'按钮name属性'，id:'按钮ID',click:function(){}//点击按钮事件｝
        format:function(value){//可以对字段的里面的值做处理。
            return value;
        }
    };
    MTable.FORMAT = {
        formatLoadingMessage: function () {
            return '正在加载中……,请稍后';
        },
        formatNoMatches:function(){//data没有数据的数据，显示的文本
            return '无符合条件的记录！';
        },
        formatShowPaginationInfo:function(totalRows,pageNumber,pageSize){
            return sprintf('当前第%s页，共%s页，合计%s条',pageNumber,(~~((totalRows - 1) / pageSize) + 1),totalRows);
        }
    };
    //合并在一起
    $.extend(MTable.DEFAULTS,MTable.FORMAT);
    MTable.prototype.init = function () {
        this.initContainer();
        this.initColumns();
        this.initHeader();
        this.initData();
        this.initHiddenRows();
        this.initPagination();
        this.initBody();
        this.initServer();
    };
    MTable.prototype.initContainer = function(){     
        this.$container = $([
            '<div class="m-table-div">',
           // '<div class="m-table-toolbar"></div>',
                '<div class="m-table-container">',
                    '<div class="m-table-header"><table></table></div>',
                    '<div class="m-table-body">',
                        '<div class="m-table-loading">',
                            this.options.formatLoadingMessage(),
                        '</div>',
                    '</div>',
                    '<div class="m-table-footer"><table><tr></tr></table></div>',
                    this.options.pagination  ? '<div class="m-table-pagination"></div>' : '',
                '</div>',
            '</div>'
        ].join(''));
        this.$container.insertAfter(this.$ele);
        this.$tableContainer = this.$container.find('.m-table-container');
        this.$tableHeader = this.$container.find('.m-table-header');
        this.$tableBody = this.$container.find('.m-table-body');
        this.$tableLoading = this.$container.find('.m-table-loading');
        //this.$tableFooter = this.$container.find('.m-table-footer');
        //this.$toolbar = this.$container.find('.m-table-toolbar');
        this.$pagination = this.$container.find('.m-table-pagination');

        this.$tableBody.append(this.$ele);
        //this.$container.after('<div class="clearfix"></div>');

        this.$ele.addClass("m-table").addClass("table-hover");
        if (this.options.striped) {
            this.$ele.addClass('table-striped');
        }
        /*if ($.inArray('table-no-bordered', this.options.classes.split(' ')) !== -1) {
            this.$tableContainer.addClass('table-no-bordered');
        }*/
    };
    MTable.prototype.initColumns = function(){     
        var that = this,
            columns = [],
            data = [];
        this.$header = $('<thead></thead>').appendTo(this.$ele);
        if (!$.isArray(this.options.columns[0])) {
            this.options.columns = [this.options.columns];
        }
        this.options.columns = $.extend(true, [], columns, this.options.columns);
        this.columns = [];
        $.each(this.options.columns, function (i, columns) {
            $.each(columns, function (j, column) {
                column = $.extend({}, MTable.COLUMN_DEFAULTS, column);
                that.options.columns[i][j] = column;
            });
        });
    };
    MTable.prototype.initHeader = function(){
        var that = this,
            visibleColumns = {},
            html = [];

        this.header = {
            fields: [],
            checkboxField:'defaultCheckBoxField'
        };

        $.each(this.options.columns, function (i, columns) {
            html.push('<tr>');
            $.each(columns, function (j, column) {
                var text = '',
                    align = '', // body align style
                    style = '',
                    class_ = sprintf(' class="%s"', column['class']),
                    unitWidth = 'px',
                    width = column.width;

                if (column.width !== undefined ) {
                    if (typeof column.width === 'string') {
                        if (column.width.indexOf('%') !== -1) {
                            unitWidth = '%';
                        }
                    }
                }

                align = sprintf('text-align: %s; ', column.align);
                style = sprintf('vertical-align: %s; ', column.valign);
                style += sprintf('width: %s; ', (column.checkbox || column.radio) && !width ?
                    '36px' : (width ? width + unitWidth : undefined));
                if (column.checkbox) {
                    that.header.checkboxField = column.field || that.header.checkboxField;
                }
                that.header.fields[i++] = column.field;
                if (!column.visible) {
                    return;
                }

                visibleColumns[column.field] = column;

                html.push('<th' + sprintf(' title="%s"', column.titleTooltip),
                    column.checkbox ?
                        sprintf(' class="bs-checkbox %s"', column['class'] || '') :
                        class_,
                    sprintf(' style="%s"', style),
                    sprintf(' data-field="%s"', column.field),
                    '>');
                html.push('<div class="th-inner">');
                text = column.title;
                if (column.checkbox) {
                    if (!that.options.singleSelect && that.options.checkboxHeader) {
                        text = '<input name="btSelectAll" type="checkbox" />';
                    }
                }
                if(column.isButtonColumn){
                    that.btnGroup = column.buttons;
                }
                html.push(text);
                html.push('</div>');
                html.push('<div class="fht-cell"></div>');
                html.push('</div>');
                html.push('</th>');
            });
            html.push('</tr>');
        });

        this.$header.html(html.join(''));
        this.$header.find('th[data-field]').each(function (i) {
            $(this).data(visibleColumns[$(this).data('field')]);
        });

        $(window).off('resize.bootstrap-table');
        if (!this.options.showHeader) {
            this.$header.hide();
            this.$tableHeader.hide();
            this.$tableLoading.css('top', 0);
        } else {
            this.$header.show();
            this.$tableHeader.show();
            this.$tableLoading.css('top', this.$header.outerHeight() + 1);
        }

        this.$selectAll = this.$header.find('[name="btSelectAll"]');
        this.$selectAll.off('click').on('click', function () {
            var checked = $(this).prop('checked');
            that[checked ? 'checkAll' : 'uncheckAll']();
        });
    };
    MTable.prototype.initData = function (data) {
        //如果有url的话，ajax调用后可以调用这个方法setdata
        this.data = data || this.options.data;
        this.options.data = this.data;
    };
    MTable.prototype.initHiddenRows = function(){    
        this.hiddenRows = [];
    }
    MTable.prototype.initBody = function () {
        var that = this,
            html = [],
            data = this.options.data;
        this.$body = this.$ele.find('>tbody');
        if (!this.$body.length) {//生成tbody标签
            this.$body = $('<tbody></tbody>').appendTo(this.$ele);
        }

        var trFragments = $(document.createDocumentFragment());
        var hasTr;

        for (var i = 0; i < data.length; i++) {
            var item = data[i];
            var tr = this.initRow(item, i, data, trFragments);
            hasTr = hasTr || !!tr;
            if (tr&&tr!==true) {
                trFragments.append(tr);
            }
        }
        
        // show no records
        if (!hasTr) {
            trFragments.append('<tr class="no-records-found">' +
                sprintf('<td colspan="%s" >%s</td>',
                    this.$header.find('th').length,
                    this.options.formatNoMatches()) +
                '</tr>');
        }

        this.$body.html(trFragments);
        this.$selectItem = this.$body.find('[name="btSelectItem"]');
        this.$selectItem.off('click').on('click', function (event) {
            var $this = $(this),
                checked = $this.prop('checked'),
                row = that.data[$this.data('index')];
            row[that.header.checkboxField] = checked;
            if (that.options.singleSelect) {
                that.$selectItem.filter(':checked').not(this).prop('checked', false);
            }
            that.updateSelected();
        });
        //绑定buttongroup事件
        this.btnGroup != undefined && this.btnGroup.length > 0 && $.each(this.btnGroup, function (i, button) {
            var fun = button.onClick || function(){};
            $.each(that.$body.find('.btn-group-td').find('.btn:eq('+i+')'),function(j){
                $(this).off('click').on('click',function(){
                    fun.call(fun,this,data[j],j);
                })
            });
        });
    };
    MTable.prototype.initRow = function(item, i, data, parentDom){       
        var that=this,
            type,
            html = [];

        html.push('<tr',
            sprintf(' data-index="%s"', i),
            '>'
        );

        $.each(this.header.fields, function(j, field) {
            var text = '',
                value = item[field] === 0 ? 0 : item[field] || that.options.undefinedText,
                column = that.options.columns[0][j];
            var text = '',
                align = '', // body align style
                style = '',
                class_ = sprintf(' class="%s"', column['class']),
                unitWidth = 'px',
                width = column.width;

            align = sprintf('text-align: %s; ', column.align);
            style = sprintf('vertical-align: %s; ', column.valign);
            if (!column.visible) {
                return;
            }
            value = column.format(value,item,i);
            if (column.checkbox) {
                type =  'checkbox';
                text = ['<td class="bs-checkbox">' ,
                    '<input' + sprintf(' data-index="%s"', i) +
                    ' name="btSelectItem"' +
                    sprintf(' type="%s"', type) +
                    sprintf(' checked="%s"', value === true || (value && value.checked)? 'checked' : undefined) +
                    sprintf(' disabled="%s"', value === true || (value && value.disabled)? 'disabled' : undefined) +
                    ' />',
                ].join('');
                //把checked记录存在data的checkboxField字段里面
                item[that.header.checkboxField] = value === true || (value && value.checked)? true : false;//
            } else if(column.isButtonColumn) {
                //按钮组
                var rowHided = value === true || (value && value.hided)? true : false;//行隐藏
                var textArr =[sprintf('<td class="btn-group-td" style="%s %s"> <div class="btn-group">',align,style)];
                if(!rowHided){
                    $.each(column.buttons, function (i, button) {
                        button.format = button.format || function(){};
                        /*var onClick = function(button,item,i){
                            button.onClick(button,item,i) || function(){};
                        };*/
                        var state = button.format(value,item,i);
                        var buttonHided = state === true || (state && state.hided)? "none" : "inline";//按钮隐藏
                        var buttonDisabled = state === true || (state && state.disabled)? "disabled" : "";//按钮不可用
                        var buttonStyleClass = state === true || (state && state.style)? state.style : "";//按钮其它样式
                    
                        textArr.push(sprintf('<button %s class="btn btn-default %s" type="button" style="display:%s;"> ',buttonDisabled,buttonStyleClass,buttonHided) +
                        //sprintf('id="$s" name="%s" title="%s" onclick="onClick('+button+','+item+','+i+')">',button.id,button.name,button.title) +
                        sprintf('id="$s" name="%s" title="%s" >',button.id,button.name,button.title) +
                        button.text+
                        '</button>');
                       
                    });
                }
                textArr.push('</div></td>');
                text = textArr.join('');
            } else {
                if('已部署' == value && 'IS_DEPLOYED' == field) {
                    text = [sprintf('<td style="%s %s %s %s" >',align,style,width ? "width:" + width : "","color: #26A65B;") +
                    value + '</td>'];
                } else {
                    text = [sprintf('<td style="%s %s %s">',align,style,width ? "width:" + width : "") +
                    value + '</td>'];
                }
               
            }

            html.push(text);
        });

        html.push('</tr>');

        return html.join(' ');
    }
    MTable.prototype.initPagination = function () {       
        if (!this.options.pagination) {
            this.$pagination.hide();
            return;
        } else {
            this.$pagination.show();
        }

        var that = this,
            html = [],
            $allSelected = false,
            i, from, to,
            $pageList,
            $first, $pre,
            $next, $last,
            $number,
            //data = this.getData(),
            pageList = this.options.pageList;

        this.totalPages = 0;
        if (this.options.totalRows) {
            this.totalPages = ~~((this.options.totalRows - 1) / this.options.pageSize) + 1;
        }
        if (this.totalPages > 0 && this.options.pageNumber > this.totalPages) {
            this.options.pageNumber = this.totalPages;
        }
        this.pageFrom = (this.options.pageNumber - 1) * this.options.pageSize + 1;
        this.pageTo = this.options.pageNumber * this.options.pageSize;
        if (this.pageTo > this.options.totalRows) {
            this.pageTo = this.options.totalRows;
        }

        html.push(
            '<div class="pull-left pagination-detail">',
            '<span class="pagination-info">',
                this.options.formatShowPaginationInfo(this.options.totalRows,this.options.pageNumber,this.options.pageSize),
            '</span></div>');

        if (!this.options.onlyInfoPagination) {
            html.push(
                '<div class="pull-right pagination">',
                    '<ul class="pagination">',
                        '<li class="page-pre"><a href="#">' + this.options.paginationPreText + '</a></li>');

            if (this.totalPages < 5) {
                from = 1;
                to = this.totalPages;
            } else {
                from = this.options.pageNumber - 2;
                to = from + 4;
                if (from < 1) {
                    from = 1;
                    to = 5;
                }
                if (to > this.totalPages) {
                    to = this.totalPages;
                    from = to - 4;
                }
            }

            if (this.totalPages >= 6) {
                if (this.options.pageNumber >= 3) {
                    html.push('<li class="page-first' + (1 === this.options.pageNumber ? ' active' : '') + '">',
                        '<a href="#">', 1, '</a>',
                        '</li>');

                    from++;
                }

                if (this.options.pageNumber >= 4) {
                    if (this.options.pageNumber == 4 || this.totalPages == 6 || this.totalPages == 7) {
                        from--;
                    } else {
                        html.push('<li class="page-first-separator disabled">',
                            '<a href="#">...</a>',
                            '</li>');
                    }

                    to--;
                }
            }

            if (this.totalPages >= 7) {
                if (this.options.pageNumber >= (this.totalPages - 2)) {
                    from--;
                }
            }

            if (this.totalPages == 6) {
                if (this.options.pageNumber >= (this.totalPages - 2)) {
                    to++;
                }
            } else if (this.totalPages >= 7) {
                if (this.totalPages == 7 || this.options.pageNumber >= (this.totalPages - 3)) {
                    to++;
                }
            }

            for (i = from; i <= to; i++) {
                html.push('<li class="page-number' + (i === this.options.pageNumber ? ' active' : '') + '">',
                    '<a href="#">', i, '</a>',
                    '</li>');
            }

            if (this.totalPages >= 8) {
                if (this.options.pageNumber <= (this.totalPages - 4)) {
                    html.push('<li class="page-last-separator disabled">',
                        '<a href="#">...</a>',
                        '</li>');
                }
            }

            if (this.totalPages >= 6) {
                if (this.options.pageNumber <= (this.totalPages - 3)) {
                    html.push('<li class="page-last' + (this.totalPages === this.options.pageNumber ? ' active' : '') + '">',
                        '<a href="#">', this.totalPages, '</a>',
                        '</li>');
                }
            }

            html.push(
                '<li class="page-next"><a href="#">' + this.options.paginationNextText + '</a></li>',
                '</ul>',
                '</div>');
        }
        this.$pagination.html(html.join(''));

        if (!this.options.onlyInfoPagination) {
            $pageList = this.$pagination.find('.page-list a');
            $first = this.$pagination.find('.page-first');
            $pre = this.$pagination.find('.page-pre');
            $next = this.$pagination.find('.page-next');
            $last = this.$pagination.find('.page-last');
            $number = this.$pagination.find('.page-number');


            // if (this.totalPages <= 1) {
            //     this.$pagination.find('div.pagination').hide();
            // }


            if (this.options.pageNumber === 1) {
                $pre.addClass('disabled');
            }
            if (this.options.pageNumber === this.totalPages) {
                $next.addClass('disabled');
            }

            //$pageList.off('click').on('click', $.proxy(this.onPageListChange, this));
            $first.off('click').on('click', $.proxy(this.onPageFirst, this));
            $pre.off('click').on('click', $.proxy(this.onPagePre, this));
            $next.off('click').on('click', $.proxy(this.onPageNext, this));
            $last.off('click').on('click', $.proxy(this.onPageLast, this));
            $number.off('click').on('click', $.proxy(this.onPageNumber, this));
        }
    };
    MTable.prototype.initServer = function (query, url ,countUrl) {
        var params = $.extend({}, query, this.options.queryParams);
        this.$tableLoading.show();
        if (!(url || this.options.url)) {
            this.load(this.options.data);
            this.$tableLoading.hide();
            return;
        }

        // 如果有countUrl存在就代表count的返回值是另外请求的，不包含在url请求路径里面
        if((countUrl || this.options.countUrl)) {
            this.initServerCount(params, countUrl || this.options.countUrl);
        }

        if (this.options.pagination) {
            params.pageSize = this.options.pageSize;
            params.pageNumber = this.options.pageNumber;
        }
        this.initServerData(params, url || this.options.url);
    };
    MTable.prototype.onPageFirst = function (event) {       
        this.options.pageNumber = 1;
        this.updatePagination(event);
        return false;
    };
    MTable.prototype.onPagePre = function (event) {        
        if ((this.options.pageNumber - 1) === 0) {
            this.options.pageNumber = this.options.totalPages;
        } else {
            this.options.pageNumber--;
        }
        this.updatePagination(event);
        return false;
    };
    MTable.prototype.onPageNext = function (event) {       
        if ((this.options.pageNumber + 1) > this.options.totalPages) {
            this.options.pageNumber = 1;
        } else {
            this.options.pageNumber++;
        }
        this.updatePagination(event);
        return false;
    };
    MTable.prototype.onPageLast = function (event) {       
        this.options.pageNumber = this.totalPages;
        this.updatePagination(event);
        return false;
    };
    MTable.prototype.onPageNumber = function (event) {       
        if (this.options.pageNumber === +$(event.currentTarget).text()) {
            return;
        }
        this.options.pageNumber = +$(event.currentTarget).text();
        this.updatePagination(event);
        return false;
    };
    MTable.prototype.updatePagination = function (event) {       
        if (event && $(event.currentTarget).hasClass('disabled')) {
            return;
        }
        this.initPagination();
        this.initServer();

        //this.trigger('page-change', this.options.pageNumber, this.options.pageSize);
    };
    MTable.prototype.initServerCount = function (query, url) {
        var that = this,
            request;
        request = {
            type: this.options.ajaxType,
            url: url,
            data: JSON.stringify(query),
            cache: this.options.ajaxCache,
            contentType: this.options.contentType,
            dataType: this.options.dataType,
            success: function (res) {
                if(res[that.options.retCode] == 0 || res[that.options.retCode]) {
                    if (res[that.options.retCode] == 0) {
                        // that.options.totalRows = res[that.options.retInfo][that.options.countCloumnName];
                        that.options.totalRows = res[that.options.retInfo];
                    } else { //TODO 失败的情况 调用回调
                        var fun = that.options.countAjaxError;
                        fun.call(fun,res);
                        return;
                    }
                }
                // else if(res[that.options.countCloumnName]){
                //     that.options.totalRows = res[that.options.countCloumnName]
                // }
                that.initPagination();
            }/*,
            error: function (res) {
                var fun = that.options.ajaxError;
                fun.call(fun,res);
                throw new Error("请求count失败");
            }*/
        };
        /*if (this._xhr && this._xhr.readyState !== 4) {
            this._xhr.abort();
        }*/
        this._xhr = $.ajax(request);

    };
    MTable.prototype.initServerData = function (query, url) {
        var that = this,
            request;
        request = {
            type: this.options.ajaxType,
            url:  url,
            data: JSON.stringify(query),
            cache: this.options.ajaxCache,
            contentType: this.options.contentType,
            dataType: this.options.dataType,
            success: function (res) {
                var data = res;
                if(res[that.options.retCode] == 0 || res[that.options.retCode]){
                    if(res[that.options.retCode] == 0){
                        data = res[that.options.retInfo];
                    }else{
                        var fun = that.options.dataAjaxError;
                        fun.call(fun,res);
                        that.$tableLoading.hide();
                        that.load([]);
                        return;
                    }
                }
                that.load(data);
                //that.trigger('load-success', res);
                that.$tableLoading.hide();
            }/*,
            error: function (res) {
                //that.trigger('load-error', res.status, res);
                var fun = that.options.ajaxError;
                fun.call(fun,res);
                that.$tableLoading.hide();
                throw new Error("请求date失败");
            }*/,
            complete : function () {
                that.$tableLoading.hide();
            }
        };

       /* if (this._xhr && this._xhr.readyState !== 4) {
            this._xhr.abort();
        }*/
        this._xhr = $.ajax(request);

    };
    MTable.prototype.load = function (data) {
        this.initData(data);
        //this.initPagination();
        this.initBody();
    };
    MTable.prototype.checkAll = function () {
        this.checkAll_(true);
    };
    MTable.prototype.uncheckAll = function () {
        this.checkAll_(false);
    };
    MTable.prototype.checkAll_ = function (checked) {
        var that = this;
        this.$selectAll.prop('checked', checked);
        this.$selectItem.filter(':enabled').prop('checked', checked);
        //把data里面的checkboxField改为全选的值
        this.$selectItem.each(function () {
            that.data[$(this).data('index')][that.header.checkboxField] = $(this).prop('checked');
        });
    };
    MTable.prototype.getSelections = function () {
        var that = this;

        return $.grep(this.options.data, function (row) {
            return row[that.header.checkboxField] === true;
        });
    };
    MTable.prototype.updateSelected = function () {
        var checkAll = this.$selectItem.filter(':enabled').length &&
            this.$selectItem.filter(':enabled').length ===
            this.$selectItem.filter(':enabled').filter(':checked').length;

        this.$selectAll.add(this.$selectAll_).prop('checked', checkAll);
    };
    MTable.prototype.proxyButtonClickEvent == function(button,row,index){

    };
    MTable.prototype.refresh = function(data){
        if(data) {
            this.options.data = data;
        }

        this.initServer();

    };
    MTable.prototype.reload = function(option){
        this.options = $.extend({},this.options,typeof option === 'object' && option);
        this.initServer();
    };
    var allowedMethods = [
        'getOptions',
        'getSelections', 'getAllSelections', 'getData',
        'load', 'reload',  'append', 'prepend', 'remove', 'removeAll',
        'insertRow', 'updateRow', 'updateCell', 'updateByUniqueId', 'removeByUniqueId',
        'getRowByUniqueId', 'showRow', 'hideRow', 'getHiddenRows',
        'mergeCells', 'refreshColumnTitle',
        'checkAll', 'uncheckAll', 'checkInvert',
        'check', 'uncheck',
        'checkBy', 'uncheckBy',
        'refresh',
        'resetView',
        'resetWidth',
        'destroy',
        'showLoading', 'hideLoading',
        'showColumn', 'hideColumn', 'getHiddenColumns', 'getVisibleColumns',
        'showAllColumns', 'hideAllColumns',
        'filterBy',
        'scrollTo',
        'getScrollPosition',
        'selectPage', 'prevPage', 'nextPage',
        'togglePagination',
        'toggleView',
        'refreshOptions',
        'resetSearch',
        'expandRow', 'collapseRow', 'expandAllRows', 'collapseAllRows',
        'updateFormatText'
    ];
    $.fn.mTable = function (option){
        var $this = $(this);
        var args = Array.prototype.slice.call(arguments, 1),//取出option后面的参数，给后面mTable函数执行
            value,//mTable函数执行的返回值
            data = $this.data('jquery.mtable'),
            options = $.extend({},MTable.DEFAULTS,$this.data,typeof option === 'object' && option);
            if(typeof option ==='string') {  //如果传进来是string类型，当成mTable函数执行
                if($.inArray(option, allowedMethods) < 0){
                    throw new Error("Unknown method: " + option);
                }
                if (!data) {
                    return;
                }
                value = data[option].apply(data, args);
            }
            if(!data){ //如果没有数据，又不是string类型，就进行初始化
                $this.data('jquery.mtable',(data = new MTable(this,options)));
            }
        return value === 'undefined' ? this : value; //如果是执行mTable函数就返回value，否则返回mTable对象本身
    };
    $.fn.mTable.Constructor = MTable;
    $.fn.mTable.defaults = MTable.DEFAULTS;
    $.fn.mTable.columnDefaults = MTable.COLUMN_DEFAULTS;
    $.fn.mTable.methods = allowedMethods;
})(jQuery);