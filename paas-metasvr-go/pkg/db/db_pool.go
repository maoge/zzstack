package db

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

type DbPool struct {
	DB              *sql.DB       `json:"db,omitempty"`
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

func (pool *DbPool) Connect() bool {
	// postgresql: sql.Open("pgx","postgres://localhost:5432/postgres")
	// mysql: username:password@tcp(ip:port)/dbname?timeout=%ds&readTimeout=%ds&charset=utf8
	connStr := fmt.Sprintf("%s:%s@tcp(%s)/%s?timeout=%ds&readTimeout=%ds",
		pool.Username, pool.Password, pool.Addr, pool.DbName, pool.ConnTimeout, pool.ReadTimeout)

	db, err := sql.Open(pool.DbType, connStr)
	if err != nil {
		log.Fatal(err)
		panic(err)
	}

	err = db.Ping()
	if err == nil {
		log.Printf("database: %v connect OK", pool.Addr)
	} else {
		log.Fatalf("database connect fail, %v", err)
		panic(err) // proper error handling instead of panic in your app
	}

	db.SetMaxOpenConns(pool.MaxOpenConns)
	db.SetMaxIdleConns(pool.MaxIdleConns)
	db.SetConnMaxLifetime(pool.ConnMaxLifetime)
	db.SetConnMaxIdleTime(pool.ConnMaxIdleTime)

	pool.DB = db

	return true
}

func (pool *DbPool) Release() {
	if pool.DB == nil {
		return
	}

	pool.DB.Close()
	pool.DB = nil
}
