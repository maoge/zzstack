package utils

import (
	"time"
)

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

func TimeFmt(ts *int64) string {
	sec := *ts / 1000
	tm := time.Unix(sec, 0)
	return tm.Format("2006-01-02 03:04:05")
}
