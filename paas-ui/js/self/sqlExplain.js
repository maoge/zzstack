/**
 * Created by guozh on 2018/8/30.
 */

(function ($,window) {
	var SQLExplain = function (options) {
		this.id = options.id;
		this.$tableEle  = options.tableEle;
		this.$sqlStrEle = options.sqlStrEle;
		this.$schemaEle = options.schemaEle;
		this.$userEle   = options.userEle;
		this.$pwdEle    = options.pwdEle;
		this.isExecute = false;
    }

    SQLExplain.prototype.execute = function (){
		let data = {
			SERV_ID : this.id,
            SQL_STR : this.$sqlStrEle.val(),
            SCHEMA_NAME : this.$schemaEle.val(),
            USER_NAME : this.$userEle.val(),
            USER_PWD : this.$pwdEle.val()
		};

        if(!this.isExecute) {
            this.$tableEle.mTable({
                url: rootUrl+'tidbsvr/sqlExplainService',
                queryParams: data,
                striped : true,
                pagination : true,
                pageSize : 20,
                pageNumber : 1,
                columns : [{
                    field : "ID",
                    title : "ID",
                    align: 'left',
                    format : function (value) {
                        return (""+value).replace(" ", "&nbsp;");
                    },
                    height:'20px'
                }, {
                    field : "COUNT",
                    title : "COUNT",
                }, {
                    field : "TASK",
                    title : "TASK",
                }, {
                    field : "OPERATOR INFO",
                    title : "OPERATOR_INFO",
                }]
            });
        }else {
            this.$tableEle.mTable("refresh");
        }

        this.isExecute = true;
	}

    window.SQLExplain = SQLExplain;
}(jQuery, window));
