package config

import "time"

type RedisConfig struct {
	Addr        string        `json:"addr,omitempty"`
	Password    string        `json:"password,omitempty"`
	MaxActive   int           `json:"max_active,omitempty"`
	MaxIdle     int           `json:"max_idle,omitempty"`
	IdleTimeout time.Duration `json:"idle_timeout,omitempty"`
}
