package pool

import (
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

// load balanced db pool

type LdbDbPool struct {
	mut           sync.Mutex
	avlDBArr      []*DbPool
	inavlDBArr    []*DbPool
	maintainTimer *time.Timer
	rrCounter     int64
}

func NewLdbDbPool(dbYaml *config.DbYaml) *LdbDbPool {
	ldbPool := new(LdbDbPool)
	ldbPool.Init(dbYaml)
	return ldbPool
}

func (ldbDbPool *LdbDbPool) Init(dbYaml *config.DbYaml) {
	ldbDbPool.mut.Lock()
	defer ldbDbPool.mut.Unlock()

	if dbYaml == nil {
		utils.LOGGER.Info(consts.ERR_LDBPOOL_YAML_INIT)
		return
	}

	dbSources := dbYaml.DbSources

	ldbDbPool.avlDBArr = make([]*DbPool, 0)
	ldbDbPool.inavlDBArr = make([]*DbPool, 0)

	for _, node := range dbSources {
		var dbPool = new(DbPool)
		dbPool.Addr = node.Addr
		dbPool.Username = node.Username
		dbPool.Password = node.Password
		dbPool.DbName = node.DbName
		dbPool.DbType = node.DbType
		dbPool.MaxOpenConns = dbYaml.MaxOpenConns
		dbPool.MaxIdleConns = dbYaml.MaxIdleConns
		dbPool.ConnTimeout = dbYaml.ConnTimeout
		dbPool.ReadTimeout = dbYaml.ReadTimeout
		dbPool.ConnMaxLifetime = time.Duration(dbYaml.ConnMaxLifetime) * time.Second
		dbPool.ConnMaxIdleTime = time.Duration(dbYaml.ConnMaxIdletime) * time.Second

		if dbPool.Connect() {
			ldbDbPool.avlDBArr = append(ldbDbPool.avlDBArr, dbPool)
		} else {
			errMsg := fmt.Sprintf("dbSource: %v connect fail ......", node.Addr)
			utils.LOGGER.Error(errMsg)

			ldbDbPool.inavlDBArr = append(ldbDbPool.inavlDBArr, dbPool)
		}
	}

	go ldbDbPool.startRecover()
}

func (ldbDbPool *LdbDbPool) Release() {
	ldbDbPool.mut.Lock()
	defer ldbDbPool.mut.Unlock()

	if ldbDbPool.maintainTimer != nil {
		ldbDbPool.maintainTimer.Stop()
		ldbDbPool.maintainTimer = nil
	}

	for _, dbPool := range ldbDbPool.avlDBArr {
		dbPool.Release()
	}
}

// select db over round-bobin balance pattern
func (ldbDbPool *LdbDbPool) GetDbPool() *DbPool {
	ldbDbPool.mut.Lock()
	defer ldbDbPool.mut.Unlock()

	avlSize := len(ldbDbPool.avlDBArr)
	idx := ldbDbPool.getDbIndex(int64(avlSize))
	dbPool := ldbDbPool.avlDBArr[idx]

	return dbPool
}

func (ldbDbPool *LdbDbPool) getDbIndex(max int64) int64 {
	if max == 0 {
		return 0
	}

	idx := atomic.AddInt64(&(ldbDbPool.rrCounter), 1)
	idx = idx % max

	return idx
}

func (ldbDbPool *LdbDbPool) startRecover() {
	ldbDbPool.maintainTimer = time.NewTimer(consts.DB_RECOVER_INTERVAL)

	for {
		select {
		case <-ldbDbPool.maintainTimer.C:
			ldbDbPool.checkDbPool()
			ldbDbPool.recoverDbPool()
			ldbDbPool.maintainTimer.Reset(consts.DB_RECOVER_INTERVAL)
		}
	}
}

func (ldbDbPool *LdbDbPool) checkDbPool() {
	ldbDbPool.mut.Lock()
	var avlLen = len(ldbDbPool.avlDBArr)
	ldbDbPool.mut.Unlock()

	if avlLen == 0 {
		return
	}

	// Ping cost too much time, so ping operation cannot be Surrounded by lock
	for i := avlLen - 1; i >= 0; i-- {
		dbPool := ldbDbPool.avlDBArr[i]

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
			ldbDbPool.inavlDBArr = append(ldbDbPool.inavlDBArr, dbPool)

			ldbDbPool.mut.Lock()
			if i > 0 {
				ldbDbPool.avlDBArr = ldbDbPool.inavlDBArr[:i]
			} else {
				ldbDbPool.avlDBArr = make([]*DbPool, 0)
			}
			ldbDbPool.mut.Unlock()
		}
	}
}

func (ldbDbPool *LdbDbPool) recoverDbPool() {
	var inavlLen = len(ldbDbPool.inavlDBArr)
	if inavlLen == 0 {
		return
	}

	for i := inavlLen - 1; i >= 0; i-- {
		dbPool := ldbDbPool.inavlDBArr[i]
		if dbPool.Connect() {
			info := fmt.Sprintf("DbPool: %v recovered ......", dbPool.Addr)
			utils.LOGGER.Info(info)

			ldbDbPool.mut.Lock()
			defer ldbDbPool.mut.Unlock()

			// remove from invalid array to valid array when recovered
			ldbDbPool.avlDBArr = append(ldbDbPool.avlDBArr, dbPool)

			if i > 0 {
				ldbDbPool.inavlDBArr = ldbDbPool.inavlDBArr[:i]
			} else {
				ldbDbPool.inavlDBArr = make([]*DbPool, 0)
			}

			break
		} else {
			errMsg := fmt.Sprintf("DbPool: %v reconnect fail ......", dbPool.Addr)
			utils.LOGGER.Error(errMsg)
		}
	}
}
