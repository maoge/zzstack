package consts

type OperationEnum int32

const (
	INSTANCE_OPERATION_START OperationEnum = iota
	INSTANCE_OPERATION_STOP
	INSTANCE_OPERATION_RESTART
	INSTANCE_OPERATION_UPDATE
)
