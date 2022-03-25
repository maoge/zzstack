package utils

import (
	"testing"
)

func TestSSHClient(t *testing.T) {
	sshClient := NewSSHClient("192.168.238.135", 22, "ultravirs", "wwwqqq.")

	sshClient.Connect()

	sshClient.Close()
}
