package consts

type EventType struct {
	CODE      int
	ALARM     bool
	WRITE_LOG bool
	INFO      string
}

var (
	EVENT_NONE                  EventType = EventType{CODE: 10000, ALARM: false, WRITE_LOG: false, INFO: "EVENT_NONE"}
	EVENT_SYNC_SESSION          EventType = EventType{CODE: 10001, ALARM: false, WRITE_LOG: false, INFO: "EVENT_SYNC_SESSION"}
	EVENT_ADD_SERVICE           EventType = EventType{CODE: 10002, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_SERVICE"}
	EVENT_MOD_SERVICE           EventType = EventType{CODE: 10003, ALARM: false, WRITE_LOG: true, INFO: "EVENT_MOD_SERVICE"}
	EVENT_DEL_SERVICE           EventType = EventType{CODE: 10004, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_SERVICE"}
	EVENT_UPD_SERVICE_DEPLOY    EventType = EventType{CODE: 10005, ALARM: false, WRITE_LOG: true, INFO: "EVENT_UPD_SERVICE_DEPLOY"}
	EVENT_ADD_INSTANCE          EventType = EventType{CODE: 10006, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_INSTANCE"}
	EVENT_DEL_INSTANCE          EventType = EventType{CODE: 10007, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_INSTANCE"}
	EVENT_UPD_INST_POS          EventType = EventType{CODE: 10008, ALARM: false, WRITE_LOG: true, INFO: "EVENT_UPD_INST_POS"}
	EVENT_UPD_INST_DEPLOY       EventType = EventType{CODE: 10009, ALARM: false, WRITE_LOG: true, INFO: "EVENT_UPD_INST_DEPLOY"}
	EVENT_ADD_INST_ATTR         EventType = EventType{CODE: 10010, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_INST_ATTR"}
	EVENT_DEL_INST_ATTR         EventType = EventType{CODE: 10011, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_INST_ATTR"}
	EVENT_ADD_TOPO              EventType = EventType{CODE: 10012, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_TOPO"}
	EVENT_DEL_TOPO              EventType = EventType{CODE: 10013, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_TOPO"}
	EVENT_MOD_TOPO              EventType = EventType{CODE: 10014, ALARM: false, WRITE_LOG: true, INFO: "EVENT_MOD_TOPO"}
	EVENT_ADD_SERVER            EventType = EventType{CODE: 10015, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_SERVER"}
	EVENT_DEL_SERVER            EventType = EventType{CODE: 10016, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_SERVER"}
	EVENT_ADD_SSH               EventType = EventType{CODE: 10017, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_SSH"}
	EVENT_DEL_SSH               EventType = EventType{CODE: 10018, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_SSH"}
	EVENT_MOD_SSH               EventType = EventType{CODE: 10019, ALARM: false, WRITE_LOG: true, INFO: "EVENT_MOD_SSH"}
	EVENT_ADD_SESSON            EventType = EventType{CODE: 10020, ALARM: false, WRITE_LOG: false, INFO: "EVENT_ADD_SESSON"}
	EVENT_REMOVE_SESSON         EventType = EventType{CODE: 10021, ALARM: false, WRITE_LOG: false, INFO: "EVENT_REMOVE_SESSON"}
	EVENT_AJUST_QUEUE_WEIGHT    EventType = EventType{CODE: 10022, ALARM: false, WRITE_LOG: true, INFO: "EVENT_AJUST_QUEUE_WEIGHT"}
	EVENT_SWITCH_DB_TYPE        EventType = EventType{CODE: 10023, ALARM: false, WRITE_LOG: true, INFO: "EVENT_SWITCH_DB_TYPE"}
	EVENT_ADD_CMPT_VER          EventType = EventType{CODE: 10024, ALARM: false, WRITE_LOG: true, INFO: "EVENT_ADD_CMPT_VER"}
	EVENT_DEL_CMPT_VER          EventType = EventType{CODE: 10025, ALARM: false, WRITE_LOG: true, INFO: "EVENT_DEL_CMPT_VER"}
	EVENT_MOD_ACC_PASSWD        EventType = EventType{CODE: 10026, ALARM: false, WRITE_LOG: true, INFO: "EVENT_MOD_ACC_PASSWD"}
	EVENT_UPD_INST_PRE_EMBEDDED EventType = EventType{CODE: 10027, ALARM: false, WRITE_LOG: true, INFO: "EVENT_UPD_INST_DEPLOY"}
	EVENT_RELOAD_METADATA       EventType = EventType{CODE: 10028, ALARM: false, WRITE_LOG: true, INFO: "EVENT_RELOAD_METADATA"}
)
