package utils

import (
	"fmt"
	"sync"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var (
	LOGGER         *zap.Logger = nil
	logger_barrier sync.Once
)

func InitLogger() {
	logger_barrier.Do(func() {
		encoderConfig := zapcore.EncoderConfig{
			TimeKey:        "time",
			LevelKey:       "level",
			NameKey:        "logger",
			CallerKey:      "caller",
			MessageKey:     "msg",
			StacktraceKey:  "stacktrace",
			LineEnding:     zapcore.DefaultLineEnding,
			EncodeLevel:    zapcore.CapitalLevelEncoder, // 大写编码器
			EncodeTime:     zapcore.ISO8601TimeEncoder,  // ISO8601 UTC 时间格式
			EncodeDuration: zapcore.SecondsDurationEncoder,
			EncodeCaller:   zapcore.ShortCallerEncoder, // 全路径编码器
		}

		atom := zap.NewAtomicLevelAt(zap.InfoLevel)
		config := zap.Config{
			Level:         atom,          // 日志级别
			Development:   false,         // 开发模式，堆栈跟踪
			Encoding:      "console",     // 输出格式 console 或 json
			EncoderConfig: encoderConfig, // 编码器配置
			// InitialFields:    map[string]interface{}{"serviceName": "paas-metasvr-go"}, // 初始化字段，如：添加一个服务器名称
			OutputPaths:      []string{"stdout", "./log/metasvr.log"}, // 输出到指定文件 stdout（标准输出，正常颜色） stderr（错误输出，红色）
			ErrorOutputPaths: []string{"stderr", "./log/metasvr.log"},
		}

		// 构建日志
		logger, err := config.Build()
		if err != nil {
			panic(fmt.Sprintf("log 初始化失败: %v", err))
		}
		logger.Info("log 初始化成功")

		LOGGER = logger
	})
}
