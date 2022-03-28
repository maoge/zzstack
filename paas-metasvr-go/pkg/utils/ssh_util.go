package utils

import (
	"fmt"
	"log"

	"golang.org/x/crypto/ssh"
	_ "golang.org/x/crypto/ssh/terminal"
)

type SSHClient struct {
	ip         string
	sshPort    int
	user       string
	passwd     string
	rawClient  *ssh.Client
	rawSession *ssh.Session
}

func NewSSHClient(ip string, sshPort int, user string, passwd string) *SSHClient {
	sshClient := new(SSHClient)
	sshClient.ip = ip
	sshClient.sshPort = sshPort
	sshClient.user = user
	sshClient.passwd = passwd
	sshClient.rawClient = nil
	sshClient.rawSession = nil

	return sshClient
}

func (h *SSHClient) Connect() bool {
	addr := fmt.Sprintf("%s:%d", h.ip, h.sshPort)
	client, err := ssh.Dial("tcp", addr, &ssh.ClientConfig{
		User:            h.user,
		Auth:            []ssh.AuthMethod{ssh.Password(h.passwd)},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	})

	if err != nil {
		// errMsg := fmt.Sprintf("Failed to dial: %s:%d, error:%s", h.ip, h.sshPort, err.Error())
		// LOGGER.Error(errMsg)
		log.Fatalf("Failed to dial: %s:%d, error:%s", h.ip, h.sshPort, err.Error())
	}

	h.rawClient = client
	h.rawSession, _ = client.NewSession()

	return true
}

func (h *SSHClient) Close() {
	if h.rawSession != nil {
		h.rawSession.Close()
		h.rawSession = nil
	}
	if h.rawClient != nil {
		h.rawClient.Close()
		h.rawClient = nil
	}
}

func (h *SSHClient) Output(cmd string) ([]byte, error) {
	return h.rawSession.Output(cmd)
}

func (h *SSHClient) Run(cmd string) {
	h.rawSession.Run(cmd)
}

// https://www.php.cn/be/go/476681.html