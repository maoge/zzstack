package proto

type PaasSsh struct {
	SSH_ID     string `db:"SSH_ID"`
	SSH_NAME   string `db:"SSH_NAME"`
	SSH_PWD    string `db:"SSH_PWD"`
	SSH_PORT   int    `db:"SSH_PORT"`
	SERV_CLAZZ string `db:"SERV_CLAZZ"`
	SERVER_IP  string `db:"SERVER_IP"`
}
