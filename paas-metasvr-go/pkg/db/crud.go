package db

import (
	"database/sql"
	"encoding/json"
	"reflect"

	"github.com/jmoiron/sqlx"
	"github.com/maoge/paas-metasvr-go/pkg/db/pool"
)

// for select single row, return outObject automatic reflect to dest struct
//     obj := Person{}
//     err := crud.SelectAsObject(db, &obj, "SELECT * FROM person WHERE first_name=?", "Jason")
func SelectAsObject(pool *pool.DbPool, outObject interface{}, sql *string, args ...interface{}) error {
	return pool.DB.Get(outObject, pool.DB.Rebind(*sql), args...)
}

func TxSelectAsObject(pool *pool.DbPool, outObject interface{}, sql *string, args ...interface{}) error {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return err
	}
	defer tx.Commit()

	return tx.Get(outObject, pool.DB.Rebind(*sql), args...)
}

// for select multi rows, return outSlice automatic reflect to dest struct
//     slice := []Person{}
//     err := SelectAsSlice(db, &slice, "SELECT * FROM person ORDER BY first_name ASC")
func SelectAsSlice(pool *pool.DbPool, outSlice interface{}, sql *string, args ...interface{}) error {
	return pool.DB.Select(outSlice, pool.DB.Rebind(*sql), args...)
}

// select with transaction
func TxSelectAsSlice(pool *pool.DbPool, outSlice interface{}, sql *string, args ...interface{}) error {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return err
	}
	defer tx.Commit()

	return tx.Select(outSlice, *sql, args...)
}

// for select single row, return result as map[string]interface{}
//     resMap, err := SelectAsMap(db, &slice, "SELECT * FROM person WHERE first_name=?", "Jason")
func SelectAsMap(pool *pool.DbPool, sql *string, args ...interface{}) (map[string]interface{}, error) {
	row := pool.DB.QueryRowx(pool.DB.Rebind(*sql), args...)

	m := map[string]interface{}{}
	err := row.MapScan(m)
	if err != nil {
		return nil, err
	}

	for k, encoded := range m {
		t := reflect.TypeOf(encoded)
		if t.Kind() == reflect.Slice {
			byteArr := encoded.([]uint8)
			m[k] = string(byteArr)
		}
	}

	return m, nil
}

func TxSelectAsMap(pool *pool.DbPool, sql *string, args ...interface{}) (map[string]interface{}, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	m := map[string]interface{}{}
	row := tx.QueryRowx(pool.DB.Rebind(*sql), args...)
	err = row.MapScan(m)
	if err != nil {
		return nil, err
	}

	for k, encoded := range m {
		t := reflect.TypeOf(encoded)
		if t.Kind() == reflect.Slice {
			byteArr := encoded.([]uint8)
			m[k] = string(byteArr)
		}
	}

	return m, nil
}

func NextSeqMargin(pool *pool.DbPool, sqlSelect *string, sqlUpdate *string, seqName string) (map[string]interface{}, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	// defer tx.Commit()

	m := map[string]interface{}{}
	row := tx.QueryRowx(pool.DB.Rebind(*sqlSelect), seqName)
	err = row.MapScan(m)
	if err != nil {
		tx.Rollback()
		return nil, err
	}

	for k, encoded := range m {
		t := reflect.TypeOf(encoded)
		if t.Kind() == reflect.Slice {
			byteArr := encoded.([]uint8)
			m[k] = string(byteArr)
		}
	}

	_, err = tx.Exec(pool.DB.Rebind(*sqlUpdate), seqName)
	if err != nil {
		tx.Rollback()
		return nil, err
	}

	tx.Commit()
	return m, nil
}

func SelectAsJson(pool *pool.DbPool, sql *string, args ...interface{}) ([]byte, error) {
	row := pool.DB.QueryRowx(pool.DB.Rebind(*sql), args...)

	m := map[string]interface{}{}
	err := row.MapScan(m)
	if err != nil {
		return nil, err
	}

	for k, encoded := range m {
		t := reflect.TypeOf(encoded)
		if t.Kind() == reflect.Slice {
			byteArr := encoded.([]uint8)
			m[k] = string(byteArr)
		}
	}

	jsonBytes, err := json.Marshal(m)
	if err != nil {
		return nil, err
	}

	return jsonBytes, nil
}

// for select multi row, return result as []map[string]interface{}
//     sliceMap, err := SelectAsMapSlice(db, sql, "SELECT * FROM person WHERE first_name=?", "Jason")
func SelectAsMapSlice(pool *pool.DbPool, sql *string, args ...interface{}) ([]interface{}, error) {
	rows, err := pool.DB.Queryx(pool.DB.Rebind(*sql), args...)
	if err != nil {
		return nil, err
	}

	mapSlice := make([]interface{}, 0)
	defer rows.Close()

	for rows.Next() {
		m := make(map[string]interface{}, 0)
		err := rows.MapScan(m)
		if err != nil {
			return nil, err
		}

		for k, encoded := range m {
			t := reflect.TypeOf(encoded)
			if t.Kind() == reflect.Slice {
				byteArr := encoded.([]uint8)
				m[k] = string(byteArr)
			}
		}

		mapSlice = append(mapSlice, m)
	}

	return mapSlice, nil
}

func SelectAsJsonArray(pool *pool.DbPool, sql *string, args ...interface{}) ([]byte, error) {
	rows, err := pool.DB.Queryx(pool.DB.Rebind(*sql), args...)
	if err != nil {
		return nil, err
	}

	mapSlice := make([]interface{}, 0)
	defer rows.Close()

	for rows.Next() {
		m := make(map[string]interface{}, 0)
		err := rows.MapScan(m)
		if err != nil {
			return nil, err
		}

		for k, encoded := range m {
			t := reflect.TypeOf(encoded)
			if t.Kind() == reflect.Slice {
				byteArr := encoded.([]uint8)
				m[k] = string(byteArr)
			}
		}

		mapSlice = append(mapSlice, m)
	}

	jsonBytes, err := json.Marshal(mapSlice)
	if err != nil {
		return nil, err
	}

	return jsonBytes, nil
}

func TxSelectAsMapSlice(pool *pool.DbPool, sql *string, args ...interface{}) ([]interface{}, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	rows, err := tx.Queryx(pool.DB.Rebind(*sql), args...)
	if err != nil {
		return nil, err
	}

	mapSlice := make([]interface{}, 0)
	defer rows.Close()

	for rows.Next() {
		m := make(map[string]interface{}, 0)
		err := rows.MapScan(m)
		if err != nil {
			return nil, err
		}

		for k, encoded := range m {
			t := reflect.TypeOf(encoded)
			if t.Kind() == reflect.Slice {
				byteArr := encoded.([]uint8)
				m[k] = string(byteArr)
			}
		}

		mapSlice = append(mapSlice, m)
	}

	return mapSlice, nil
}

// for insert with param list bind with ?
//     "INSERT INTO person (first_name, last_name, email) VALUES (?, ?, ?)"
func Insert(pool *pool.DbPool, sql *string, args ...any) (sql.Result, error) {
	return pool.DB.Exec(pool.DB.Rebind(*sql), args...)
}

func TxInsert(pool *pool.DbPool, sql *string, args ...any) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.Exec(pool.DB.Rebind(*sql), args...)
}

// for insert with named param bind:
//     "INSERT INTO person (first_name, last_name, email) VALUES (:first, :last, :email)"
func NamedInsert(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	return pool.DB.NamedExec(*sql, *args)
}

func TxNamedInsert(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.NamedExec(*sql, *args)
}

// for update with param list bind with ?
//     "update person set first_name = ?, last_name = ?, email = ? where id = ?"
func Update(pool *pool.DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	return pool.DB.Exec(pool.DB.Rebind(*sql), args...)
}

func UpdateOri(db *sqlx.DB, sql *string, args ...interface{}) (sql.Result, error) {
	newSql := db.Rebind(*sql)
	return db.Exec(newSql, args...)
}

func TxUpdate(pool *pool.DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.Exec(pool.DB.Rebind(*sql), args...)
}

// for update with named param bind:
//     "update person set first_name = :first, last_name = :last, email = :email where id = :id"
func NamedUpdate(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	return pool.DB.NamedExec(*sql, *args)
}

func TxNamedUpdate(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.NamedExec(*sql, *args)
}

// for delete with param list bind with ?
//     "delete from person where id = ?"
func Delete(pool *pool.DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	return pool.DB.Exec(pool.DB.Rebind(*sql), args...)
}

func TxDelete(pool *pool.DbPool, sql *string, args ...interface{}) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.Exec(pool.DB.Rebind(*sql), args...)
}

// for delete with named param bind:
//     "delete from person where id = :id"
func NamedDelete(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	return pool.DB.NamedExec(*sql, *args)
}

func TxNamedDelete(pool *pool.DbPool, sql *string, args *map[string]interface{}) (sql.Result, error) {
	tx, err := pool.DB.Beginx()
	if err != nil {
		return nil, err
	}
	defer tx.Commit()

	return tx.NamedExec(*sql, *args)
}
