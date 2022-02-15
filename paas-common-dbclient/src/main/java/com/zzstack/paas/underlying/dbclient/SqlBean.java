package com.zzstack.paas.underlying.dbclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SqlBean {

    private String sql;
    private List<Object> params;

    public SqlBean() {
    }

    public SqlBean(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params;
    }

    public SqlBean(String sql) {
        this.sql = sql;
    }

    public void putParam(Object param) {
        if (params == null)
            params = new ArrayList<Object>();
        params.add(param);
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    public void addParams(Object[] obj) {
        this.params = Arrays.asList(obj);
    }

    public List<Object> getParams() {
        return params;
    }

}
