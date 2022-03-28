package utils

import (
	"log"
	"testing"
)

func TestSSHClient(t *testing.T) {
	sshClient := NewSSHClient("127.0.0.1", 22, "ultravirs", "wwwqqq.")

	sshClient.Connect()

	bytes, err := sshClient.Output("ls -al")
	if err == nil {
		log.Printf("%s", string(bytes))
	}

	sshClient.Close()
}
