package utils

import (
	"fmt"
)

func GeneratePasswd(accName, passwd string) string {
	concatAccPasswd := fmt.Sprintf("%v|%v", accName, passwd)
	return MD5V(concatAccPasswd)
}
