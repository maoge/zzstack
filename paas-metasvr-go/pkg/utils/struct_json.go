package utils

import (
	"encoding/json"
	"fmt"
	"log"
)

func Struct2Json(val interface{}) string {
	jsonBytes, errs := json.Marshal(val)
	if errs != nil {
		fmt.Println(errs.Error())
		log.Fatalf("json marshal err, %v", val)
	}

	s := string(jsonBytes)
	return s
}

func Json2Struct(data []byte, dest interface{}) {
	json.Unmarshal(data, dest)
}
