package proto

import (
	"strconv"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PaasMetaCmpt struct {
	CMPT_ID        int    `db:"CMPT_ID"`
	CMPT_NAME      string `db:"CMPT_NAME"`
	CMPT_NAME_CN   string `db:"CMPT_NAME_CN"`
	IS_NEED_DEPLOY string `db:"IS_NEED_DEPLOY"`
	SERV_TYPE      string `db:"SERV_TYPE"`
	SERV_CLAZZ     string `db:"SERV_CLAZZ"`
	NODE_JSON_TYPE string `db:"NODE_JSON_TYPE"`
	SUB_CMPT_ID    string `db:"SUB_CMPT_ID"`
	SUB_CMPT_SET   map[int]bool
}

func (m *PaasMetaCmpt) InitSubCmptSet() {
	m.SUB_CMPT_SET = make(map[int]bool)
	if m.SUB_CMPT_ID == "" {
		return
	}

	arr := strings.Split(m.SUB_CMPT_ID, ",")
	for _, s := range arr {
		s = strings.Trim(s, " ")
		cmptId, err := strconv.ParseInt(s, 10, 32)
		if err == nil {
			m.SUB_CMPT_SET[int(cmptId)] = true
		} else {
			recover()
		}
	}

	utils.Struct2Json(m)
}

func (m *PaasMetaCmpt) HaveSubComponent() bool {
	return m.SUB_CMPT_SET != nil && len(m.SUB_CMPT_SET) > 0
}

func (m *PaasMetaCmpt) IsNeedDeploy() bool {
	return m.IS_NEED_DEPLOY == consts.STR_TRUE
}
