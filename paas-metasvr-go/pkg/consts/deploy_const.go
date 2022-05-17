package consts

const (
	REG_VERSION                    = "%VERSION%"
	TAR_GZ_SURFIX                  = ".tar.gz"
	ZIP_SURFIX                     = ".zip"
	TAR_ZXVF                       = "-zxvf"
	SHELL_UNTERMINATED             = "unterminated"
	END_DALLAR_BLANK               = "$ "
	END_MORE_BLANK                 = "> "
	END_BRACKET_DALLAR             = "]$"
	REDIS_CLUSTER_INIT_ACCEPT      = "(type 'yes' to accept): "
	REDIS_CLUSTER_INIT_OK          = "[OK] All 16384 slots covered."
	REDIS_CLUSTER_INIT_ERR         = "ERR"
	REDIS_CLUSTER_DELETE_NODE      = ">>> Sending CLUSTER RESET SOFT to the deleted node."
	REDIS_ADD_NODE_OK              = "[OK] New node added correctly."
	REDIS_SLAVEOF_OK               = "OK"
	REDIS_MOVEING_SLOT             = "Moving slot"
	COMMON_JDK_OK                  = "openjdk version \"1.8.0_181\""
	TAOS_CMD_END                   = "taos> "
	TAOS_SHELL_END                 = "Query OK"
	TAOS_DNODE_READY               = "ready"
	TAOS_DNODE_DROPPING            = "dropping"
	VOLTDB_CONN_FAIL               = "Unable to connect to VoltDB cluster"
	START_SHELL                    = "start.sh"
	STOP_SHELL                     = "stop.sh"
	STOP_NOAUTH_SHELL              = "stop_noauth.sh"
	ARBITRATOR_START_SHELL         = "arbitrator_start.sh"
	ARBITRATOR_STOP_SHELL          = "arbitrator_stop.sh"
	TAOSD_START_SHELL              = "taosd_start.sh"
	TAOSD_STOP_SHELL               = "taosd_stop.sh"
	YB_MASTER_START_SHELL          = "master_start.sh"
	YB_MASTER_STOP_SHELL           = "master_stop.sh"
	YB_TSERVER_START_SHELL         = "tserver_start.sh"
	YB_TSERVER_STOP_SHELL          = "tserver_stop.sh"
	YB_TSERVER_CONF                = "./etc/yb-tserver.conf"
	YB_MASTER_CONF                 = "./etc/yb-master.conf"
	PD_DELETE_MEMBER_SUCC          = "Success!"
	PD_DELETE_STORE_SUCC           = "Success!"
	TIKV_OFFLINE_STATUS            = "Offline"
	TIKV_TOMBSTONE_STATUS          = "Tombstone"
	REDIS_CLUSTER_REPLICAS         = 1
	REDIS_CLUSTER_TTL_SLOT         = 16384
	REDIS_ROLE_MASTER              = 1
	REDIS_ROLE_SLAVE               = 0
	REDIS_ROLE_NONE                = -1
	REDIS_CLUSTER_MIN_MASTER_NODES = 3
	CPU_SLICE                      = 50000
	CMPT_COLLECTD                  = "COLLECTD"
	CMPT_REDIS_NODE                = "REDIS_NODE"
	CMPT_REDIS_PROXY               = "REDIS_PROXY"
)

const (
	CHECK_PORT_RETRY int = 1000
)
