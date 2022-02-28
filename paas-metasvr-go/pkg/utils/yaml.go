package utils

import (
	"io/ioutil"
	"log"
	"os"

	"gopkg.in/yaml.v2"
)

func Unmarshal(file string) map[string]interface{} {
	var hdr, err = os.OpenFile(file, os.O_RDONLY, 0444)
	if err != nil {
		log.Fatalf("error: %v", err)
		return nil
	}

	defer hdr.Close()

	fd, err := ioutil.ReadAll(hdr)
	if err != nil {
		log.Fatalf("read to fd fail, %v", err)
		return nil
	}

	result := make(map[string]interface{})
	err = yaml.Unmarshal([]byte(fd), &result)
	if err != nil {
		log.Fatalf("error: %v", err)
		return nil
	}

	return result
}

func Marshal(data map[string]interface{}) []byte {
	return nil
}
