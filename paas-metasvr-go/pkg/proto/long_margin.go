package proto

import (
	"sync"
)

type LongMargin struct {
	Start int64
	End   int64
	Curr  int64
	mut   sync.Mutex
}

func NewLongMargin(start, end int64) *LongMargin {
	margin := new(LongMargin)
	margin.Start = start
	margin.Curr = start
	margin.End = end
	return margin
}

func (h *LongMargin) NextId() int64 {
	if h.Curr > h.End {
		return -1
	}

	h.mut.Lock()
	defer h.mut.Unlock()

	res := h.Curr
	h.Curr++
	return res
}
