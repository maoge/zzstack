package db

import (
	"log"
	"sync"
	"sync/atomic"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

// load balanced db pool

type LdbDbPool struct {
	mut          sync.Mutex
	avlDBArr     []DbPool
	inavlDBArr   []DbPool
	recoverTimer *time.Timer
	rrCounter    int64
}

func (ldbDbPool *LdbDbPool) Init(dbYaml *config.DbYaml) {
	ldbDbPool.mut.Lock()
	defer ldbDbPool.mut.Unlock()

	if dbYaml == nil {
		log.Fatalln("LdbDbPool Init error, dbYaml nil ......")
		return
	}

	dbSources := dbYaml.DbSources

	ldbDbPool.avlDBArr = make([]DbPool, 0)
	ldbDbPool.inavlDBArr = make([]DbPool, 0)

	for _, node := range dbSources {
		var dbPool = DbPool{
			Addr:     node.Addr,
			Username: node.Username,
			Password: node.Password,
			DbName:   node.DbName,
			DbType:   node.DbType,

			MaxOpenConns:    dbYaml.MaxOpenConns,
			MaxIdleConns:    dbYaml.MaxIdleConns,
			ConnTimeout:     dbYaml.ConnTimeout,
			ReadTimeout:     dbYaml.ReadTimeout,
			ConnMaxLifetime: time.Duration(dbYaml.ConnMaxLifetime) * time.Second,
			ConnMaxIdleTime: time.Duration(dbYaml.ConnMaxIdletime) * time.Second,
		}

		if dbPool.Connect() {
			ldbDbPool.avlDBArr = append(ldbDbPool.avlDBArr, dbPool)
		} else {
			log.Fatalf("dbSource: %v connect fail ......", node)
			ldbDbPool.inavlDBArr = append(ldbDbPool.inavlDBArr, dbPool)
		}
	}

	go ldbDbPool.startRecover()
}

func (ldbDbPool *LdbDbPool) Release() {
	ldbDbPool.mut.Lock()
	defer ldbDbPool.mut.Unlock()

	if ldbDbPool.recoverTimer != nil {
		ldbDbPool.recoverTimer.Stop()
		ldbDbPool.recoverTimer = nil
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
	dbPool := &ldbDbPool.avlDBArr[idx]

	return dbPool
}

func (ldbDbPool *LdbDbPool) getDbIndex(max int64) int64 {
	if max == 0 {
		return 0
	}

	idx := atomic.AddInt64(&ldbDbPool.rrCounter, 1)
	idx = idx % max

	return idx
}

func (ldbDbPool *LdbDbPool) startRecover() {
	ldbDbPool.recoverTimer = time.NewTimer(consts.DB_RECOVER_INTERVAL)

	for {
		select {
		case <-ldbDbPool.recoverTimer.C:
			ldbDbPool.recoverDbPool()
			ldbDbPool.recoverTimer.Reset(consts.DB_RECOVER_INTERVAL)
		}
	}
}

func (ldbDbPool *LdbDbPool) recoverDbPool() {
	var inavlLen = len(ldbDbPool.inavlDBArr)
	if inavlLen == 0 {
		return
	}

	for i := inavlLen - 1; i >= 0; i-- {
		dbPool := &ldbDbPool.inavlDBArr[i]
		if dbPool.Connect() {
			log.Printf("DbPool %v recovered ......", dbPool)

			ldbDbPool.mut.Lock()
			defer ldbDbPool.mut.Unlock()

			// remove from broken array to useable array when recovered
			ldbDbPool.avlDBArr = append(ldbDbPool.avlDBArr, *dbPool)

			if i > 0 {
				ldbDbPool.inavlDBArr = ldbDbPool.inavlDBArr[:i]
			} else {
				ldbDbPool.inavlDBArr = make([]DbPool, 0)
			}

			break
		} else {
			log.Fatalf("DbPool %v reconnect fail ......", dbPool)
		}
	}
}
