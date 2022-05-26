package utils

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
)

func PostJson(url string, postData *string) []byte {
	req, err := http.NewRequest("POST", url, bytes.NewReader([]byte(*postData)))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		errMsg := fmt.Sprintf("PostJson url:{%s}, postData:{%s}, error: %s", url, *postData, err.Error())
		LOGGER.Error(errMsg)
		return nil
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		errMsg := fmt.Sprintf("PostJson url:{%s}, postData:{%s}, error: %s", url, *postData, err.Error())
		LOGGER.Error(errMsg)
		return nil
	}

	// fmt.Println("response Body:", string(body))
	return body
}
