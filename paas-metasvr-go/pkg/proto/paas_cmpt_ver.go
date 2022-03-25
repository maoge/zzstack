package proto

import (
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

type PaasCmptVer struct {
	SERV_TYPE string `db:"SERV_TYPE"`
	VERSION   string `db:"VERSION"`
	VER_LIST  []string
}

func NewPaasCmptVer(servType string, version string) *PaasCmptVer {
	cmptVer := new(PaasCmptVer)
	cmptVer.SERV_TYPE = servType
	cmptVer.VERSION = version

	cmptVer.InitVerList()
	return cmptVer
}

func (m *PaasCmptVer) InitVerList() {
	s := strings.Trim(m.VERSION, " ")
	m.VER_LIST = make([]string, 0)
	m.VER_LIST = append(m.VER_LIST, s)
}

func (m *PaasCmptVer) AddVersion(version string) {
	m.VER_LIST = append(m.VER_LIST, version)
}

func (m *PaasCmptVer) DelVersion(version string) {
	length := len(m.VER_LIST)
	if length == 0 {
		return
	}

	tmp := make([]string, length-1)
	for _, s := range m.VER_LIST {
		if s == version {
			continue
		}

		tmp = append(tmp, s)
	}

	m.VER_LIST = tmp
}

func (m *PaasCmptVer) IsVersionExist(ver string) bool {
	for _, v := range m.VER_LIST {
		if ver == v {
			return true
		}
	}
	return false
}

func (m *PaasCmptVer) ToJsonMap() map[string]interface{} {
	versionSlice := ""
	for _, s := range m.VER_LIST {
		if versionSlice != "" {
			versionSlice += ","
		}

		versionSlice += s
	}

	result := make(map[string]interface{})
	result[consts.HEADER_SERV_TYPE] = m.SERV_TYPE
	result[consts.HEADER_VERSION] = versionSlice
	return result
}
