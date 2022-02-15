package com.zzstack.paas.underlying.metasvr.eventbus;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum EventType {

    EVENT_NONE                 (10000, false, false, "EVENT_NONE"),
    EVENT_SYNC_SESSION         (10001, false, false, "EVENT_SYNC_SESSION"),
    EVENT_ADD_SERVICE          (10002, false, true,  "EVENT_ADD_SERVICE"),
    EVENT_MOD_SERVICE          (10003, false, true,  "EVENT_MOD_SERVICE"),
    EVENT_DEL_SERVICE          (10004, false, true,  "EVENT_DEL_SERVICE"),
    EVENT_UPD_SERVICE_DEPLOY   (10005, false, true,  "EVENT_UPD_SERVICE_DEPLOY"),
    EVENT_ADD_INSTANCE         (10006, false, true,  "EVENT_ADD_INSTANCE"),
    EVENT_DEL_INSTANCE         (10007, false, true,  "EVENT_DEL_INSTANCE"),
    EVENT_UPD_INST_POS         (10008, false, true,  "EVENT_UPD_INST_POS"),
    EVENT_UPD_INST_DEPLOY      (10009, false, true,  "EVENT_UPD_INST_DEPLOY"),
    EVENT_ADD_INST_ATTR        (10010, false, true,  "EVENT_ADD_INST_ATTR"),
    EVENT_DEL_INST_ATTR        (10011, false, true,  "EVENT_DEL_INST_ATTR"),
    EVENT_ADD_TOPO             (10012, false, true,  "EVENT_ADD_TOPO"),
    EVENT_DEL_TOPO             (10013, false, true,  "EVENT_DEL_TOPO"),
    EVENT_MOD_TOPO             (10014, false, true,  "EVENT_MOD_TOPO"),
    EVENT_ADD_SERVER           (10015, false, true,  "EVENT_ADD_SERVER"),
    EVENT_DEL_SERVER           (10016, false, true,  "EVENT_DEL_SERVER"),
    EVENT_ADD_SSH              (10017, false, true,  "EVENT_ADD_SSH"),
    EVENT_DEL_SSH              (10018, false, true,  "EVENT_DEL_SSH"),
    EVENT_MOD_SSH              (10019, false, true,  "EVENT_MOD_SSH"),
    EVENT_ADD_SESSON           (10020, false, false, "EVENT_ADD_SESSON"),
    EVENT_REMOVE_SESSON        (10021, false, false, "EVENT_REMOVE_SESSON"),
    EVENT_AJUST_QUEUE_WEIGHT   (10022, false, true,  "EVENT_AJUST_QUEUE_WEIGHT"),
    EVENT_SWITCH_DB_TYPE       (10023, false, true,  "EVENT_SWITCH_DB_TYPE"),
    EVENT_ADD_CMPT_VER         (10024, false, true,  "EVENT_ADD_CMPT_VER"),
    EVENT_DEL_CMPT_VER         (10025, false, true,  "EVENT_DEL_CMPT_VER"),
    EVENT_MOD_ACC_PASSWD       (10026, false, true,  "EVENT_MOD_ACC_PASSWD"),
    EVENT_UPD_INST_PRE_EMBEDDED(10027, false, true,  "EVENT_UPD_INST_DEPLOY"),
    EVENT_RELOAD_METADATA      (10028, false, true,  "EVENT_RELOAD_METADATA");

	private final int     code;
	private final boolean alarm;
	private final boolean needWriteOperLog;
	private final String  info;

	private static final Map<Integer,EventType> map = new HashMap<Integer,EventType>();

	static {
		for (EventType s : EnumSet.allOf(EventType.class)) {
			map.put(s.code, s);
		}
	}

	private EventType(int iCode, boolean bAlarm, boolean bNeedWriteOperLog, String sInfo) {
		code             = iCode;
		alarm            = bAlarm;
		needWriteOperLog = bNeedWriteOperLog;
		info             = sInfo;
	}

	public static EventType get(int code){
		return map.get(code);
	}

	public int getCode() {
		return code;
	}

	public boolean isAarm() {
		return alarm;
	}

	public boolean isNeedWriteOperLog() {
	    return needWriteOperLog;
	}

	public String getInfo() {
		return info;
	}

	public boolean equals(final EventType e) {
		return this.code == e.code;
	}

}
