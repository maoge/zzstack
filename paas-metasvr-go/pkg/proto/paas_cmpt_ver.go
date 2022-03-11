package proto

import (
	"strings"
)

type PaasCmptVer struct {
	SERV_TYPE string `db:"SERV_TYPE"`
	VERSION   string `db:"VERSION"`
	VER_LIST  []string
}

func (m *PaasCmptVer) InitVerList() {
	s := strings.Trim(m.VERSION, " ")
	m.VER_LIST = make([]string, 0)
	m.VER_LIST = append(m.VER_LIST, s)
}

func (m *PaasCmptVer) AddVersion(version string) {
	m.VER_LIST = append(m.VER_LIST, version)
}
