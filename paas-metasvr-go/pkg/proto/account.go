package proto

type Account struct {
	ACC_ID      string `db:"ACC_ID"`
	ACC_NAME    string `db:"ACC_NAME"`
	PHONE_NUM   string `db:"PHONE_NUM"`
	MAIL        string `db:"MAIL"`
	PASSWD      string `db:"PASSWD"`
	CREATE_TIME uint64 `db:"CREATE_TIME"`
}
