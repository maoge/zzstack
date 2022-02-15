package com.zzstack.paas.underlying.dao;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.utils.DBInUse.EnumDBInUse;

import io.vertx.core.json.JsonObject;

public class ActiveStandbyDBDao {
    
    public static void switchDBType(String dbType, String dbName, JsonObject json) {
        EnumDBInUse dbInUse = dbType.equals(EnumDBInUse.master.name()) ? EnumDBInUse.master : EnumDBInUse.backup;
        boolean result = ActiveStandbyDBSrcPool.switchDBType(dbName, dbInUse);
        json.put("result", result);
    }
    
    public static void dbInUse(String dbName, JsonObject json) {
        EnumDBInUse dbInUse = ActiveStandbyDBSrcPool.getDBInUse(dbName);
        json.put("DBInUse", dbInUse.name());
    }

}
