package proto

import (
	"strconv"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

type PaasRedisNode struct {
	NodeId    string
	Ip        string
	Port      string
	RedisRole int
	SlotRange string
	MasterId  string
	SlaveIds  []string
}

func NewPaasRedisNode(nodeId, ip, port string) *PaasRedisNode {
	paasRedisNode := new(PaasRedisNode)
	paasRedisNode.RedisRole = consts.REDIS_ROLE_NONE
	paasRedisNode.SlaveIds = make([]string, 0)

	paasRedisNode.NodeId = nodeId
	paasRedisNode.Ip = ip
	paasRedisNode.Port = port

	return paasRedisNode
}

func (h *PaasRedisNode) AddSlaveId(slaveId string) {
	h.SlaveIds = append(h.SlaveIds, slaveId)
}

func (h *PaasRedisNode) IsSlaveEmpty() bool {
	return len(h.SlaveIds) == 0
}

func (h *PaasRedisNode) GetSlotCount() int {
	// eg:0-1364 5461-6825 10923-12287
	vs := strings.Split(h.SlotRange, " ")
	var slotCnt int = 0
	for _, str := range vs {
		if str == "" {
			continue
		}

		splitIdx := strings.Index(str, "-")
		if splitIdx == -1 {
			slotCnt++
		}

		sStart := str[:splitIdx]
		sEnd := str[splitIdx+1:]

		start, _ := strconv.Atoi(sStart)
		end, _ := strconv.Atoi(sEnd)
		margin := end - start + 1
		slotCnt += margin
	}

	return slotCnt
}
