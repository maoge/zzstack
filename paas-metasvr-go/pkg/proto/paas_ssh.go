package proto

import "github.com/maoge/paas-metasvr-go/pkg/utils"

type PaasSsh struct {
	SSH_ID     string `db:"SSH_ID"`
	SSH_NAME   string `db:"SSH_NAME"`
	SSH_PWD    string `db:"SSH_PWD"`
	SSH_PORT   int    `db:"SSH_PORT"`
	SERV_CLAZZ string `db:"SERV_CLAZZ"`
	SERVER_IP  string `db:"SERVER_IP"`
}

func NewPaasSsh(sshId string, sshName string, sshPwd string, sshPort int, servClazz string, serverIp string) *PaasSsh {
	ssh := new(PaasSsh)
	ssh.SSH_ID = sshId
	ssh.SSH_NAME = sshName
	ssh.SSH_PWD = sshPwd
	ssh.SSH_PORT = sshPort
	ssh.SERV_CLAZZ = servClazz
	ssh.SERVER_IP = serverIp

	return ssh
}

func ParsePaasSSH(msg string) *PaasSsh {
	ssh := new(PaasSsh)
	utils.Json2Struct([]byte(msg), ssh)
	return ssh
}
