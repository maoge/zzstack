package proto

import (
	_ "github.com/go-playground/validator/v10"
)

// type LoginParam struct {
// 	USER     string `json:"USER" validate:"required,min=3,max=32"`
// 	PASSWORD string `json:"PASSWORD" validate:"required,min=8,max=128"`
// }

// check int range
// int `json:"age" binding:"gte=1,lte=120"`

// value enum:
// string `validate:"oneof=female male"`

// check email format
// string `json:"email" binding:"required,email"

type Count struct {
	COUNT int64 `db:"COUNT"`
}

type LoginParam struct {
	USER     string `json:"USER" binding:"required,min=3,max=32"`
	PASSWORD string `json:"PASSWORD" binding:"required,min=8,max=128"`
}

type ModPasswdParam struct {
	MAGIC_KEY string `json:"USER" binding:"required,min=32,max=48"`
	PASSWORD  string `json:"PASSWORD" binding:"required,min=8,max=128"`
}

type GetOpLogCntParam struct {
	USER     string `json:"USER" binding:"required"`
	START_TS int64  `json:"START_TS" binding:"required"`
	END_TS   int64  `json:"END_TS" binding:"required"`
}

type GetOpLogListParam struct {
	USER        string `json:"USER" binding:"required"`
	START_TS    int64  `json:"START_TS" binding:"required"`
	END_TS      int64  `json:"END_TS" binding:"required"`
	PAGE_SIZE   int    `json:"PAGE_SIZE" binding:"required"`
	PAGE_NUMBER int    `json:"PAGE_NUMBER" binding:"required"`
}

type GetAlarmCountParam struct {
	DEAL_FLAG string `json:"DEAL_FLAG" binding:"omitempty"`
}

type GetAlarmListParam struct {
	SERV_INST_ID string `json:"SERV_INST_ID" binding:"omitempty"`
	INST_ID      string `json:"INST_ID" binding:"omitempty"`
	DEAL_FLAG    string `json:"DEAL_FLAG" binding:"omitempty,oneof=0 1"`
	PAGE_SIZE    int    `json:"PAGE_SIZE" binding:"required"`
	PAGE_NUMBER  int    `json:"PAGE_NUMBER" binding:"required"`
}

type ClearAlarmParam struct {
	ALARM_ID      int    `json:"ALARM_ID" binding:"required"`
	INST_ID       string `json:"INST_ID" binding:"required"`
	ALARM_TYPE    int    `json:"ALARM_TYPE" binding:"required"`
	DEAL_ACC_NAME string `json:"DEAL_ACC_NAME" binding:"required"`
}

type GetServiceCountParam struct {
	SERV_NAME  string `json:"SERV_NAME" binding:"omitempty"`
	SERV_CLAZZ string `json:"SERV_CLAZZ" binding:"omitempty"`
	SERV_TYPE  string `json:"SERV_TYPE" binding:"omitempty"`
}

type GetServiceListParam struct {
	SERV_INST_ID string `json:"SERV_INST_ID" binding:"omitempty"`
	SERV_NAME    string `json:"SERV_NAME" binding:"omitempty"`
	SERV_CLAZZ   string `json:"SERV_CLAZZ" binding:"omitempty"`
	SERV_TYPE    string `json:"SERV_TYPE" binding:"omitempty"`
	PAGE_SIZE    int    `json:"PAGE_SIZE" binding:"required"`
	PAGE_NUMBER  int    `json:"PAGE_NUMBER" binding:"required"`
}

type GetServTypeVerCountParam struct {
	SERV_TYPE string `json:"SERV_TYPE" binding:"omitempty"`
}

type GetServTypeVerListByPageParam struct {
	SERV_TYPE   string `json:"SERV_TYPE" binding:"omitempty"`
	PAGE_SIZE   int    `json:"PAGE_SIZE" binding:"required"`
	PAGE_NUMBER int    `json:"PAGE_NUMBER" binding:"required"`
}

type GetDashboardAddrParam struct {
	SERV_INST_ID string `json:"SERV_INST_ID" binding:"required"`
}

type AddServiceParam struct {
	SERV_NAME  string `json:"SERV_NAME" binding:"required"`
	SERV_CLAZZ string `json:"SERV_CLAZZ" binding:"required"`
	SERV_TYPE  string `json:"SERV_TYPE" binding:"required"`
	VERSION    string `json:"VERSION" binding:"required"`
	IS_PRODUCT string `json:"IS_PRODUCT" binding:"required"`
	USER       string `json:"USER" binding:"required"`
	PASSWORD   string `json:"PASSWORD" binding:"required"`
}

type DelServiceParam struct {
	INST_ID string `json:"INST_ID" binding:"required"`
}
