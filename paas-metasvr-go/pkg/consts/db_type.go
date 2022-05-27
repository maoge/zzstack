package consts

type DbType struct {
	DBName     string
	DriverName string
}

var (
	DBTYPE_MYSQL DbType = DbType{DBName: "mysql", DriverName: "mysql"}
	DBTYPE_PG    DbType = DbType{DBName: "postgresql", DriverName: "pgx"}
)
