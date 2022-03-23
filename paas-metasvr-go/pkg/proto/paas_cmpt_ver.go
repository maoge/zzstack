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

func (m *PaasCmptVer) InitVerList() {
	s := strings.Trim(m.VERSION, " ")
	m.VER_LIST = make([]string, 0)
	m.VER_LIST = append(m.VER_LIST, s)
}

func (m *PaasCmptVer) AddVersion(version string) {
	m.VER_LIST = append(m.VER_LIST, version)
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
