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
	x := int((*posMap)[consts.HEADER_X].(float64))
	y := int((*posMap)[consts.HEADER_Y].(float64))

	rowRaw := (*posMap)["row"]
	colRaw := (*posMap)["col"]
	widthRaw := (*posMap)["width"]
	heightRaw := (*posMap)["height"]

	pos := NewPaasPos(x, y)
	if rowRaw != nil {
		pos.Row = int(rowRaw.(float64))
	}
	if colRaw != nil {
		pos.Col = int(colRaw.(float64))
	}
	if widthRaw != nil {
		pos.Width = int(widthRaw.(float64))
	}
	if heightRaw != nil {
		pos.Height = int(heightRaw.(float64))
	}

	return pos
}
