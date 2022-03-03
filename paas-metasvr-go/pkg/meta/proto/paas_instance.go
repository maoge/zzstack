package proto

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

type PaasInstance struct {
	INST_ID string `db:"INST_ID"`
	CMPT_ID uint32 `db:"CMPT_ID"`
	STATUS  string `db:"IS_DEPLOYED"`
	POS_X   int32  `db:"POS_X"`
	POS_Y   int32  `db:"POS_Y"`
	WIDTH   int32  `db:"WIDTH"`
	HEIGHT  int32  `db:"HEIGHT"`
	ROW_    int32  `db:"ROW_"`
	COL_    int32  `db:"COL_"`
}

func (m *PaasInstance) IsDeployed() bool {
	return m.STATUS != consts.STR_SAVED
}

func (m *PaasInstance) IsDefaultPos() bool {
	return m.POS_X == 0 && m.POS_Y == 0 && m.WIDTH == consts.POS_DEFAULT_VALUE && m.HEIGHT == consts.POS_DEFAULT_VALUE && m.ROW_ == consts.POS_DEFAULT_VALUE && m.COL_ == consts.POS_DEFAULT_VALUE
}
