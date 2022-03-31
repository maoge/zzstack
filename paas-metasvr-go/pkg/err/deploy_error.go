package err

type DeployErr struct {
	ErrInfo string
}

func (e DeployErr) Error() string {
	return e.ErrInfo
}
