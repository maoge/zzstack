package err

type SqlErr struct {
	ErrInfo string
}

func (sqlErr SqlErr) Error() string {
	return sqlErr.ErrInfo
}
