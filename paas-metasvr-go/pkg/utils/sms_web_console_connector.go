package utils

import (
	"fmt"
	"net"
)

type SmsWebConsoleConnector struct {
	IP     string
	Port   int
	Passwd string
	conn   *net.TCPConn
}

func (h *SmsWebConsoleConnector) Connect() bool {
	remoteAddr := fmt.Sprintf("%s:%d", h.IP, h.Port)
	tcpAddr, err := net.ResolveTCPAddr("tcp4", remoteAddr)
	if err != nil {
		return false
	}

	h.conn, err = net.DialTCP("tcp4", nil, tcpAddr)
	if err != nil {
		return false
	}

	return true
}

func (h *SmsWebConsoleConnector) SendData(message []byte) bool {
	_, err := h.conn.Write(message)
	if err != nil {
		return true
	} else {
		return false
	}
}

func (h *SmsWebConsoleConnector) Close() {
	if h.conn != nil {
		h.conn.Close()
		h.conn = nil
	}
}
