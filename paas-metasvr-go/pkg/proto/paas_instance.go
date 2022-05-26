package proto

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PaasInstance struct {
	INST_ID string `db:"INST_ID"`
	CMPT_ID int    `db:"CMPT_ID"`
	STATUS  string `db:"IS_DEPLOYED"`
	POS_X   int    `db:"POS_X"`
	POS_Y   int    `db:"POS_Y"`
	WIDTH   int    `db:"WIDTH"`
	HEIGHT  int    `db:"HEIGHT"`
	ROW     int    `db:"ROW_"`
	COL     int    `db:"COL_"`
}

func NewPaasInstance(instId string, cmptId int, status string, x int, y int, width int, height int, row int, col int) *PaasInstance {
	instance := new(PaasInstance)

	instance.INST_ID = instId
	instance.CMPT_ID = cmptId
	instance.STATUS = status
	instance.POS_X = x
	instance.POS_Y = y
	instance.WIDTH = width
	instance.HEIGHT = height
	instance.ROW = row
	instance.COL = col

	return instance
}

func (m *PaasInstance) IsDeployed() bool {
	return m.STATUS != consts.STR_SAVED
}

func (m *PaasInstance) IsDefaultPos() bool {
	return m.POS_X == 0 && m.POS_Y == 0 &&
		m.WIDTH == consts.POS_DEFAULT_VALUE && m.HEIGHT == consts.POS_DEFAULT_VALUE &&
		m.ROW == consts.POS_DEFAULT_VALUE && m.COL == consts.POS_DEFAULT_VALUE
}

func ParsePaasInstance(s string) *PaasInstance {
	paasInstance := new(PaasInstance)
	utils.Json2Struct([]byte(s), paasInstance)
	return paasInstance
}
