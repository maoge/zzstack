package proto

type PaasDeployFile struct {
	FILE_ID     int    `db:"FILE_ID"`
	HOST_ID     int    `db:"HOST_ID"`
	SERV_TYPE   string `db:"SERV_TYPE"`
	VERSION     string `db:"VERSION"`
	FILE_NAME   string `db:"FILE_NAME"`
	FILE_DIR    string `db:"FILE_DIR"`
	CREATE_TIME uint64 `db:"CREATE_TIME"`
}
