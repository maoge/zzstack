package consts

import (
	"time"
)

var (
	DB_RECOVER_INTERVAL time.Duration = 3 * time.Second

	INFO_OK                 = "OK"
	STR_TRUE                = "1"
	STR_FALSE               = "0"
	STR_ALARM               = "4"
	STR_ERROR               = "3"
	STR_WARN                = "2"
	STR_DEPLOYED            = "1"
	STR_SAVED               = "0"
	POS_DEFAULT_VALUE int32 = -1

	ERR_LDBPOOL_YAML_INIT = "LdbDbPool Init error, dbYaml nil ......"
)
