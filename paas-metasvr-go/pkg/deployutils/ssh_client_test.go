package deployutils

import (
	"log"
	"testing"

	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func TestSSHClient(t *testing.T) {
	sshClient := NewSSHClient("192.168.238.135", 22, "ultravirs", "wwwqqq.")

	sshClient.Connect()

	paasResult := result.NewResultBean()
	if sshClient.SCP("sms", "wlwx2021", "172.20.0.171", "22", "/home/sms/ftp/redis-5.0.2.tar.gz", "./redis-5.0.2.tar.gz", "", paasResult) {
		log.Printf("scp ok")
	}

	sshClient.Close()
}
