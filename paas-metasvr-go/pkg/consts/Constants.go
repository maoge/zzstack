package consts

import (
	"time"
)

var (
	DB_RECOVER_INTERVAL time.Duration = 3 * time.Second

	ERR_LDBPOOL_YAML_INIT = "LdbDbPool Init error, dbYaml nil ......"
)
