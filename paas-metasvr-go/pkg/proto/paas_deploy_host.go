package proto

type PaasDeployHost struct {
	HOST_ID     int    `db:"HOST_ID"`
	IP_ADDRESS  string `db:"IP_ADDRESS"`
	USER_NAME   string `db:"USER_NAME"`
	USER_PWD    string `db:"USER_PWD"`
	SSH_PORT    string `db:"SSH_PORT"`
	CREATE_TIME uint64 `db:"CREATE_TIME"`
}
