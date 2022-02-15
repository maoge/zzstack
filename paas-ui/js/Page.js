/**
 * Created by guozh on 2018/4/10.
 */
(function($, container){
    function Page(pageDir, mTableParam, showModalButtons){
        this.pageDir = pageDir;
        var self = this;
        container.load(pageDir,function(){
            $(".breadcrumb>li>a").off("click").on("click",function(){
                var $this = $(this), pageName = $this.data("load-page"),$main = $("#mainContent");
                if(pageName != ""){
                    $(".contextMenu").remove();
                    new Page(pageName+".html");
                }
            });
            self.afterLoad(mTableParam);
        });
    }

    Page.prototype.afterLoad = function(mTableParam){
        this.$mTable = $(".mTable");
        this.mTableParam = mTableParam;
        this.init();
    }

    Page.prototype.init = function(){
        var mtable = this.$mTable,
            param = this.mTableParam;
        mtable.mTable(param);
    }

    Page.prototype.refreshTable = function(){
        this.$mTable.mTable("refresh");
    }

    Page.prototype.reLoadTable = function(param){
        this.$mTable.mTable("reload",param);
    }

    Page.prototype.showModal = function(){
        $(".modal").modal("show");
        $(".modal-backdrop").appendTo($("#mainContent"));
    }

})(jQuery, $mainContainer);