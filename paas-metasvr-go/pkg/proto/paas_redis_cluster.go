package proto

import (
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

type PaasRedisCluster struct {
	MasterNodes map[string]*PaasRedisNode
	SlaveNodes  map[string]*PaasRedisNode
}

func NewPaasRedisCluster() *PaasRedisCluster {
	paasRedisCluster := new(PaasRedisCluster)
	paasRedisCluster.MasterNodes = make(map[string]*PaasRedisNode)
	paasRedisCluster.SlaveNodes = make(map[string]*PaasRedisNode)
	return paasRedisCluster
}

func (h *PaasRedisCluster) Parse(info string) {
	vs := strings.Split("info", "\n")

	for _, str := range vs {
		idxAt := strings.Index(str, "@")
		if idxAt == -1 {
			continue
		}

		size := len(str)
		if str[size-1:] == "\r" {
			str = str[:size-2]
		}

		// d5d27d0379b28a1c0965d5ee3fdead018e204c9a 172.20.0.171:13002@23002 master - 0 1649230545419 2 connected 5461-10922
		// b91f8eec7570e89c8f24ce97f2cb5d18c588415c 172.20.0.171:13001@23001 myself,master - 0 1649230545000 1 connected 0-5460
		// e1d627314e5aee042801f4a3fbd24503e8b41415 172.20.0.171:13003@23003 master - 0 1649230544000 3 connected 10923-16383
		// dd73ea25bab1aa271b93786a660cd927c924fd34 172.20.0.171:13004@23004 slave d5d27d0379b28a1c0965d5ee3fdead018e204c9a 0 1649230543000 4 connected
		// 992e816a7bc2d65443b60270f3d03173a62d492e 172.20.0.171:13005@23005 slave e1d627314e5aee042801f4a3fbd24503e8b41415 0 1649230546419 5 connected
		// 5226b1db08fe711d9dc24b5d1c09c09a9a669564 172.20.0.171:13006@23006 slave b91f8eec7570e89c8f24ce97f2cb5d18c588415c 0 1649230544416 6 connected

		idxAddr := strings.Index(str, " ")
		idxSem := strings.Index(str, ":")

		nodeId := str[:idxAddr]
		ip := str[idxAddr+1 : idxSem]
		port := str[idxSem+1 : idxAt]

		node := NewPaasRedisNode(nodeId, ip, port)

		if strings.Index(str, "master") != -1 {
			node.RedisRole = consts.REDIS_ROLE_MASTER

			idxSlot := strings.Index(str, "connected ")
			if idxSlot != -1 {
				start := idxSlot + len("connected ")
				node.SlotRange = str[start:]
			}

			h.MasterNodes[node.NodeId] = node
		} else {
			node.RedisRole = consts.REDIS_ROLE_SLAVE

			idxIdBeg := strings.Index(str, "slave ") + len("slave ")
			subStr := str[idxIdBeg:]
			idxIdEnd := strings.LastIndex(subStr, " ")

			masterId := subStr[:idxIdEnd]
			node.MasterId = masterId

			h.SlaveNodes[node.NodeId] = node
		}

	}

	for _, slave := range h.SlaveNodes {
		master, found := h.MasterNodes[slave.MasterId]
		if found {
			master.AddSlaveId(slave.NodeId)
		}
	}
}

func (h *PaasRedisCluster) GetSelfInfo(ip, port string) *PaasRedisNode {
	for _, node := range h.MasterNodes {
		if node.Ip == ip && node.Port == port {
			return node
		}
	}

	for _, node := range h.SlaveNodes {
		if node.Ip == ip && node.Port == port {
			return node
		}
	}

	return nil
}

func (h *PaasRedisCluster) GetSlaves(masterId string) []*PaasRedisNode {
	result := make([]*PaasRedisNode, 0)
	master, found := h.MasterNodes[masterId]
	if !found {
		return result
	}

	slaveIds := master.SlaveIds
	for _, slaveId := range slaveIds {
		slave := h.SlaveNodes[slaveId]
		result = append(result, slave)
	}

	return result
}

func (h *PaasRedisCluster) GetMasters() []*PaasRedisNode {
	result := make([]*PaasRedisNode, 0)
	for _, node := range h.MasterNodes {
		result = append(result, node)
	}

	return result
}

func (h *PaasRedisCluster) GetAloneMaster() string {
	for _, node := range h.MasterNodes {
		if node.IsSlaveEmpty() {
			return node.NodeId
		}
	}
	return ""
}

func (h *PaasRedisCluster) GetAnyMasterAddr() (string, bool) {
	for _, master := range h.MasterNodes {
		return master.Ip + ":" + master.Port, true
	}

	return "", false
}
