package config

import (
	"log"

	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type DbNode struct {
	Addr     string `json:"addr"`
	Username string `json:"username"`
	Password string `json:"password"`
	DbName   string `json:"dbname"`
	DbType   string `json:"dbtype"`
}

type DbYaml struct {
	Decrypt bool   `json:"decrypt,omitempty"`
	DbType  string `json:"db_type,omitempty"`

	DbSources []DbNode `json:"db_sources,omitempty"`

	ConnMaxLifetime int `json:"conn_max_lifetime,omitempty"`
	ConnMaxIdletime int `json:"conn_max_idletime,omitempty"`
	ConnTimeout     int `json:"conn_timeout,omitempty"`
	ReadTimeout     int `json:"read_timeout,omitempty"`
	MaxIdleConns    int `json:"max_idle_conns,omitempty"`
	MaxOpenConns    int `json:"max_open_conns,omitempty"`
}

// parse db yaml format config to struct: DbYaml
func (dbYaml *DbYaml) Load(file string) {
	rootMap := utils.Unmarshal(file)

	if rootMap == nil {
		log.Fatalf("load %v to fd fail", file)
		return
	}

	var dbYamlMap = rootMap["dbYaml"].(map[interface{}]interface{})
	if dbYamlMap == nil {
		log.Fatalf("file %v dbYaml nil", file)
		return
	}

	dbYaml.Decrypt = dbYamlMap["decrypt"].(bool)
	dbYaml.DbType = dbYamlMap["dbType"].(string)

	dbYaml.ConnMaxLifetime = dbYamlMap["connMaxLifetime"].(int)
	dbYaml.ConnMaxIdletime = dbYamlMap["connMaxIdletime"].(int)
	dbYaml.ConnTimeout = dbYamlMap["connTimeout"].(int)
	dbYaml.ReadTimeout = dbYamlMap["readTimeout"].(int)
	dbYaml.MaxIdleConns = dbYamlMap["maxIdleConns"].(int)
	dbYaml.MaxOpenConns = dbYamlMap["maxOpenConns"].(int)

	dbNodeArr := dbYamlMap["dbSources"].([]interface{})
	if dbNodeArr != nil {
		dbSourceLen := len(dbNodeArr)
		dbYaml.DbSources = make([]DbNode, dbSourceLen)
		dbYaml.parseDbSources(dbYaml.DbType, dbNodeArr)
	} else {
		dbYaml.DbSources = nil
	}
}

func (dbYaml *DbYaml) parseDbSources(dbType string, nodes []interface{}) {
	if nodes == nil {
		return
	}

	for i, v := range nodes {
		item := v.(map[interface{}]interface{})
		dbYaml.DbSources[i] = parseDbNode(dbType, item)
	}
}

func parseDbNode(dbType string, v map[interface{}]interface{}) DbNode {
	if v == nil {
		return DbNode{}
	}

	addr := v["addr"].(string)
	username := v["username"].(string)
	password := v["password"].(string)
	dbname := v["dbname"].(string)

	return DbNode{
		Addr:     addr,
		Username: username,
		Password: password,
		DbName:   dbname,
		DbType:   dbType,
	}
}
