package load_balanced_pool

import (
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type MultiPool struct {
	mut           sync.Mutex
	avlDBArr      []*NodePool
	inavlDBArr    []*NodePool
	maintainTimer *time.Timer
	rrCounter     int64
}

func NewMultiPool(dbYaml *config.DbYaml) *MultiPool {
	multiPool := new(MultiPool)
	multiPool.Init(dbYaml)
	return multiPool
}

func (multiPool *MultiPool) Init(dbYaml *config.DbYaml) {
	multiPool.mut.Lock()
	defer multiPool.mut.Unlock()

	if dbYaml == nil {
		utils.LOGGER.Info(consts.ERR_LDBPOOL_YAML_INIT)
		return
	}

	dbSources := dbYaml.DbSources

	multiPool.avlDBArr = make([]*NodePool, 0)
	multiPool.inavlDBArr = make([]*NodePool, 0)

	for _, node := range dbSources {
		var nodePool = new(NodePool)
		nodePool.Addr = node.Addr
		nodePool.Username = node.Username
		nodePool.Password = node.Password
		nodePool.DbName = node.DbName
		nodePool.DbType = node.DbType
		nodePool.MaxOpenConns = dbYaml.MaxOpenConns
		nodePool.MaxIdleConns = dbYaml.MaxIdleConns
		nodePool.ConnTimeout = dbYaml.ConnTimeout
		nodePool.ReadTimeout = dbYaml.ReadTimeout
		nodePool.ConnMaxLifetime = time.Duration(dbYaml.ConnMaxLifetime) * time.Second
		nodePool.ConnMaxIdleTime = time.Duration(dbYaml.ConnMaxIdletime) * time.Second

		if nodePool.Connect() {
			multiPool.avlDBArr = append(multiPool.avlDBArr, nodePool)
		} else {
			errMsg := fmt.Sprintf("dbSource: %v connect fail ......", node.Addr)
			utils.LOGGER.Error(errMsg)

			multiPool.inavlDBArr = append(multiPool.inavlDBArr, nodePool)
		}
	}

	go multiPool.startRecover()
}

func (multiPool *MultiPool) Release() {
	multiPool.mut.Lock()
	defer multiPool.mut.Unlock()

	if multiPool.maintainTimer != nil {
		multiPool.maintainTimer.Stop()
		multiPool.maintainTimer = nil
	}

	for _, node := range multiPool.avlDBArr {
		node.Release()
	}
}

// select db over round-bobin balance pattern
func (multiPool *MultiPool) GetDbPool() *NodePool {
	multiPool.mut.Lock()
	defer multiPool.mut.Unlock()

	avlSize := len(multiPool.avlDBArr)
	idx := multiPool.getDbIndex(int64(avlSize))
	dbPool := multiPool.avlDBArr[idx]

	return dbPool
}

func (multiPool *MultiPool) getDbIndex(max int64) int64 {
	if max == 0 {
		return 0
	}

	idx := atomic.AddInt64(&(multiPool.rrCounter), 1)
	idx = idx % max

	return idx
}

func (multiPool *MultiPool) startRecover() {
	multiPool.maintainTimer = time.NewTimer(consts.DB_RECOVER_INTERVAL)

	for {
		select {
		case <-multiPool.maintainTimer.C:
			multiPool.checkDbPool()
			multiPool.recoverDbPool()
			multiPool.maintainTimer.Reset(consts.DB_RECOVER_INTERVAL)
		}
	}
}

func (multiPool *MultiPool) checkDbPool() {
	multiPool.mut.Lock()
	var avlLen = len(multiPool.avlDBArr)
	multiPool.mut.Unlock()

	if avlLen == 0 {
		return
	}

	// Ping cost too much time, so ping operation cannot be Surrounded by lock
	for i := avlLen - 1; i >= 0; i-- {
		dbPool := multiPool.avlDBArr[i]

		if dbPool.DB == nil {
			errInfo := fmt.Sprintf("ldbDbPool avlDBArr[%v].DB nil ......", i)
			utils.LOGGER.Error(errInfo)
			continue
		}

		err := dbPool.DB.Ping()
		if err != nil {
			errMsg := fmt.Sprintf("DbPool %v disconnected ......", dbPool.Addr)
			utils.LOGGER.Error(errMsg)

			dbPool.DB.Close()
			dbPool.DB = nil

			// remove from valid array to invalid array when broken
			multiPool.inavlDBArr = append(multiPool.inavlDBArr, dbPool)

			multiPool.mut.Lock()
			if i > 0 {
				multiPool.avlDBArr = multiPool.inavlDBArr[:i]
			} else {
				multiPool.avlDBArr = make([]*NodePool, 0)
			}
			multiPool.mut.Unlock()
		}
	}
}

func (multiPool *MultiPool) recoverDbPool() {
	var inavlLen = len(multiPool.inavlDBArr)
	if inavlLen == 0 {
		return
	}

	for i := inavlLen - 1; i >= 0; i-- {
		node := multiPool.inavlDBArr[i]
		if node.Connect() {
			info := fmt.Sprintf("NodePool: %v recovered ......", node.Addr)
			utils.LOGGER.Info(info)

			multiPool.mut.Lock()
			defer multiPool.mut.Unlock()

			// remove from invalid array to valid array when recovered
			multiPool.avlDBArr = append(multiPool.avlDBArr, node)

			if i > 0 {
				multiPool.inavlDBArr = multiPool.inavlDBArr[:i]
			} else {
				multiPool.inavlDBArr = make([]*NodePool, 0)
			}

			break
		} else {
			errMsg := fmt.Sprintf("DbPool: %v reconnect fail ......", node.Addr)
			utils.LOGGER.Error(errMsg)
		}
	}
}
