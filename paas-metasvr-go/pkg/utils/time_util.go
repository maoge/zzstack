package utils

import "time"

func CurrentTimeSec() int64 {
	return time.Now().Unix()
}

func CurrentTimeMilli() int64 {
	return time.Now().UnixMilli()
}

func CurrentTimeMicro() int64 {
	return time.Now().UnixMicro()
}

func CurrentTimeNano() int64 {
	return time.Now().UnixNano()
}
