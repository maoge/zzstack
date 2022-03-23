package proto

import "github.com/maoge/paas-metasvr-go/pkg/consts"

type PaasPos struct {
	X      int
	Y      int
	Width  int
	Height int
	Row    int
	Col    int
}

func NewPaasPos(x, y int) *PaasPos {
	pos := new(PaasPos)
	pos.X = x
	pos.Y = y
	pos.Width = consts.POS_DEFAULT_VALUE
	pos.Height = consts.POS_DEFAULT_VALUE
	pos.Row = consts.POS_DEFAULT_VALUE
	pos.Col = consts.POS_DEFAULT_VALUE

	return pos
}

func GetPos(posMap *map[string]interface{}) *PaasPos {
	x := (*posMap)[consts.HEADER_X].(int)
	y := (*posMap)[consts.HEADER_Y].(int)

	rowRaw := (*posMap)["row"]
	colRaw := (*posMap)["col"]
	widthRaw := (*posMap)["width"]
	heightRaw := (*posMap)["height"]

	pos := NewPaasPos(x, y)
	if rowRaw != nil {
		pos.Row = rowRaw.(int)
	}
	if colRaw != nil {
		pos.Col = colRaw.(int)
	}
	if widthRaw != nil {
		pos.Width = widthRaw.(int)
	}
	if heightRaw != nil {
		pos.Height = heightRaw.(int)
	}

	return pos
}
