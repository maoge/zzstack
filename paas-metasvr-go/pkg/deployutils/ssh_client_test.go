package deployutils

import (
	"log"
	"testing"

	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func TestSSHClient(t *testing.T) {
	sshClient := NewSSHClient("192.168.238.135", 22, "ultravirs", "wwwqqq.")

	sshClient.Connect()

	bytes, err := sshClient.ExecCmd("ls -al")
	if err == nil {
		log.Printf("%s", string(bytes))
	}

	paasResult := result.NewResultBean()
	if sshClient.SCP("sms", "wlwx2021", "172.20.0.171", "22", "/home/sms/ftp/yugabyte-2.9.0.tar.gz", "./work/yugabyte-2.9.0.tar.gz", "", paasResult) {
		log.Printf("scp ok")
	}

	sshClient.Close()
}
