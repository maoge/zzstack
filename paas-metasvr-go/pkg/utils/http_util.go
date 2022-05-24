package utils

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
)

func PostJson(url string, postData *string) {
	req, err := http.NewRequest("POST", url, bytes.NewReader([]byte(*postData)))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		errMsg := fmt.Sprintf("PostJson url:{%s}, postData:{%s}, error: %s", url, *postData, err.Error())
		LOGGER.Error(errMsg)
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		errMsg := fmt.Sprintf("PostJson url:{%s}, postData:{%s}, error: %s", url, *postData, err.Error())
		LOGGER.Error(errMsg)
	}

	fmt.Println("response Body:", string(body))
}
