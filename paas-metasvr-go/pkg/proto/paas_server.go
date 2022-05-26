package proto

import "github.com/maoge/paas-metasvr-go/pkg/utils"

type PaasServer struct {
	SERVER_IP   string `db:"SERVER_IP"`
	SERVER_NAME string `db:"SERVER_NAME"`
}

func ParsePaasServer(msg string) *PaasServer {
	paasServer := new(PaasServer)
	utils.Json2Struct([]byte(msg), paasServer)
	return paasServer
}
