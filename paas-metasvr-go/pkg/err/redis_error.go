package err

type RedisErr struct {
	ErrInfo string
}

func (e RedisErr) Error() string {
	return e.ErrInfo
}
