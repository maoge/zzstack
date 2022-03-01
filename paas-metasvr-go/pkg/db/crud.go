package db

import (
	"database/sql"
	"log"

	"github.com/maoge/paas-metasvr-go/pkg/err"
)

type CRUD struct {
}

// 查询单行记录
func (crud *CRUD) QueryRow(pool *DbPool, sql *string, args ...interface{}) (map[string]interface{}, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD QueryRow pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD QueryRow Prepare error: %v", err)
		return nil, err
	}

	rows, err := stmt.Query(args...)
	defer rows.Close()
	if err != nil {
		// log.Fatalf("CRUD QueryRow Query error: %v", err)
		log.Println(err.Error())
		return nil, err
	}

	if rows == nil {
		log.Fatalf("CRUD QueryRow Query rows nil ......")
		return nil, nil
	}

	columns, _ := rows.Columns()
	columnLength := len(columns)
	cache := make([]interface{}, columnLength) //临时存储每行数据
	for index := range cache {                 //为每一列初始化一个指针
		var a interface{}
		cache[index] = &a
	}
	var result map[string]interface{} = nil //返回的切片
	if rows.Next() {
		err = rows.Scan(cache...)
		if err == nil {
			result = make(map[string]interface{})
			for i, colVal := range cache {
				result[columns[i]] = *colVal.(*interface{}) //取实际类型
			}
		} else {
			log.Fatalf("CRUD QueryRow rows Scan error: %v", err)
			return nil, err
		}
	}
	return result, nil
}

// 查询返回记录列表
func (crud *CRUD) QueryList(pool *DbPool, sql *string, args ...interface{}) ([]map[string]interface{}, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD QueryList pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		return nil, err
	}

	rows, err := stmt.Query(args)
	defer rows.Close()
	if err != nil {
		return nil, err
	}

	if rows == nil {
		log.Fatalf("CRUD QueryList Query rows nil ......")
		return nil, nil
	}

	columns, _ := rows.Columns()
	columnLength := len(columns)
	cache := make([]interface{}, columnLength) //临时存储每行数据
	for index := range cache {                 //为每一列初始化一个指针
		var a interface{}
		cache[index] = &a
	}
	var list []map[string]interface{} //返回的切片
	for rows.Next() {
		err = rows.Scan(cache...)
		if err == nil {
			item := make(map[string]interface{})
			for i, data := range cache {
				item[columns[i]] = *data.(*interface{}) //取实际类型
			}
			list = append(list, item)
		} else {
			log.Fatalf("CRUD QueryList rows.Scan error: %v", err)
			return nil, err
		}
	}
	return list, nil
}

// count()
func (crud *CRUD) QueryCount(pool *DbPool, sql *string, args ...interface{}) (int, error) {
	if pool == nil {
		return 0, &err.SqlErr{ErrInfo: "CRUD QueryCount pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("QueryCount Prepare error: %v", err)
		return 0, err
	}

	row := stmt.QueryRow(args)

	var cnt int
	err = row.Scan(&cnt)
	if err != nil {
		return 0, err
	}

	return cnt, nil
}

// 更新
func (crud *CRUD) Update(pool *DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD Update pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD Update Prepare error: %v", err)
		return nil, err
	}

	return stmt.Exec()
}

// tx更新
func (crud *CRUD) TxUpdate(pool *DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD TxUpdate pool nil ......"}
	}

	db := pool.DB
	tx, err := db.Begin()
	if err != nil {
		return nil, err
	}

	stmt, err := tx.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD TxUpdate Prepare error: %v", err)
		return nil, err
	}

	result, err := stmt.Exec(args...)
	if err != nil {
		tx.Rollback()
		return nil, err
	} else {
		err = tx.Commit()
		if err != nil {
			log.Fatalf("CRUD TxUpdate Commit error: %v", err)
			return nil, err
		}
	}

	return result, err
}

// 单条插入
func (crud *CRUD) Insert(pool *DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD Insert pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD Insert Prepare error: %v", err)
		return nil, err
	}

	return stmt.Exec(args)
}

// tx单条插入
func (crud *CRUD) TxInsert(pool *DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD TxInsert pool nil ......"}
	}

	db := pool.DB
	tx, err := db.Begin()
	if err != nil {
		log.Fatalf("CRUD TxInsert Begin error: %v", err)
		return nil, err
	}

	stmt, err := tx.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD TxInsert Prepare error: %v", err)
		return nil, err
	}

	result, err := stmt.Exec(args...)
	if err != nil {
		tx.Rollback()
		return nil, err
	} else {
		err = tx.Commit()
		if err != nil {
			log.Fatalf("CRUD TxInsert Commit error: %v", err)
			return nil, err
		}
	}

	return result, err
}

// batch插入
func (crud *CRUD) BatchInsert(pool *DbPool, sql *string, args []interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD BatchInsert pool nil ......"}
	}

	db := pool.DB
	stmt, err := db.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD BatchInsert Prepare error: %v", err)
		return nil, err
	}

	return stmt.Exec(args...)
}

// tx batch插入
func (crud *CRUD) TxBatchInsert(pool *DbPool, sql *string, args []interface{}) (sql.Result, error) {
	if pool == nil {
		return nil, &err.SqlErr{ErrInfo: "CRUD TxBatchInsert pool nil ......"}
	}

	db := pool.DB
	tx, err := db.Begin()
	if err != nil {
		log.Fatalf("CRUD TxBatchInsert tx Begin error: %v", err)
		return nil, err
	}

	stmt, err := tx.Prepare(*sql)
	defer stmt.Close()
	if err != nil {
		log.Fatalf("CRUD TxBatchInsert tx Prepare error: %v", err)
		return nil, err
	}

	result, err := stmt.Exec(args...)
	if err != nil {
		tx.Rollback()
		return nil, err
	} else {
		err = tx.Commit()
		if err != nil {
			log.Fatalf("CRUD TxBatchInsert tx Commit error: %v", err)
			return nil, err
		}
	}

	return result, err
}
