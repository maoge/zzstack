package utils

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
)

func MD5V(str string) string {
	h := md5.New()
	h.Write([]byte(str))
	return hex.EncodeToString(h.Sum(nil))
}

func GeneratePasswd(accName, passwd string) string {
	concatAccPasswd := fmt.Sprintf("%v|%v", accName, passwd)
	return MD5V(concatAccPasswd)
}
