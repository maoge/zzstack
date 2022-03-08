package utils

import (
	uuid "github.com/satori/go.uuid"
)

func GenUUID() string {
	id := uuid.NewV4()
	return id.String()
}
