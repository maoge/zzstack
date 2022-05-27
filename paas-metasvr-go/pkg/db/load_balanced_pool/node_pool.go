package load_balanced_pool

import (
	"database/sql"
	"fmt"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type NodePool struct {
	DB              *sql.DB
	Addr            string        `json:"addr,omitempty"`
	Username        string        `json:"username,omitempty"`
	Password        string        `json:"password,omitempty"`
	DbName          string        `json:"dbname,omitempty"`
	DbType          string        `json:"dbtype,omitempty"`
	MaxOpenConns    int           `json:"max_open_conns,omitempty"`
	MaxIdleConns    int           `json:"max_idle_conns,omitempty"`
	ConnTimeout     int           `json:"conn_timeout,omitempty"`
	ReadTimeout     int           `json:"read_timeout,omitempty"`
	ConnMaxLifetime time.Duration `json:"conn_max_lifetime,omitempty"`
	ConnMaxIdleTime time.Duration `json:"conn_max_idle_time,omitempty"`
}

func (pool *NodePool) GetConnStr() (string, string) {
	connStr := ""
	driver := ""
	switch pool.DbType {
	case consts.DBTYPE_MYSQL.DBName:
		connStr = fmt.Sprintf("%s:%s@tcp(%s)/%s?timeout=%ds&readTimeout=%ds&charset=utf8",
			pool.Username, pool.Password, pool.Addr, pool.DbName, pool.ConnTimeout, pool.ReadTimeout)
		driver = consts.DBTYPE_MYSQL.DriverName
		break
	case consts.DBTYPE_PG.DBName:
		connStr = fmt.Sprintf("%s:%s@tcp(%s)/%s?timeout=%ds&readTimeout=%ds",
			pool.Username, pool.Password, pool.Addr, pool.DbName, pool.ConnTimeout, pool.ReadTimeout)
		driver = consts.DBTYPE_PG.DriverName
		break
	default:
		break
	}
	return connStr, driver
}

func (pool *NodePool) Connect() bool {
	connStr, driver := pool.GetConnStr()

	result := false
	db, err := sql.Open(driver, connStr)
	if err == nil {
		info := fmt.Sprintf("database: %v connect OK", pool.Addr)
		utils.LOGGER.Info(info)

		db.SetMaxOpenConns(pool.MaxOpenConns)
		db.SetMaxIdleConns(pool.MaxIdleConns)
		db.SetConnMaxLifetime(pool.ConnMaxLifetime)
		db.SetConnMaxIdleTime(pool.ConnMaxIdleTime)

		if db.Ping() == nil {
			pool.DB = db
			result = true
		}
	} else {
		err := fmt.Sprintf("database connect fail, %v", err.Error())
		utils.LOGGER.Error(err)
	}

	return result
}

func (pool *NodePool) Release() {
	if pool.DB != nil {
		pool.DB.Close()
		pool.DB = nil
	}
}
