package sequence

import (
	"errors"
	"fmt"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
)

var (
	SEQ         *SequenceId = nil
	seq_barrier sync.Once
)

type SequenceId struct {
	seqMap map[string]*proto.LongMargin
}

func InitSeqence() {
	seq_barrier.Do(func() {
		SEQ = NewSequenceId()
	})
}

func NewSequenceId() *SequenceId {
	seq := new(SequenceId)
	seq.seqMap = make(map[string]*proto.LongMargin)
	return seq
}

func (h *SequenceId) NextId(seqName string) (int64, error) {
	margin := h.seqMap[seqName]
	if margin == nil {
		margin, err := metadao.GetNextSeqMargin(seqName, consts.DEFAULT_SEQ_STEP)
		if err != nil {
			return -1, err
		}

		if margin != nil {
			h.seqMap[seqName] = margin
		} else {
			return -1, errors.New(fmt.Sprintf("%s %s", seqName, consts.ERR_SEQ_NOT_EXISTS))
		}
	}

	id := margin.NextId()
	if id == -1 {
		// id取完了，拉取下一步长
		margin, err := metadao.GetNextSeqMargin(seqName, consts.DEFAULT_SEQ_STEP)
		if err != nil {
			return -1, err
		}

		h.seqMap[seqName] = margin
		id = margin.NextId()
	}

	return id, nil
}
