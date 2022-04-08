package utils

import (
	"container/list"
	"strings"
	"sync"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type DeployLog struct {
	mut           sync.Mutex
	logMap        map[string]*LogItem
	maintainTimer *time.Timer
}

type LogItem struct {
	mut      sync.Mutex
	TS       int64
	LogQueue *list.List
}

const (
	EXPIRE_TIME                  = 600000
	CLEAN_INTERVAL time.Duration = 60 * time.Second
)

func NewDeployLog() *DeployLog {
	deployLog := new(DeployLog)
	deployLog.logMap = make(map[string]*LogItem)

	go deployLog.startClean()

	return deployLog
}

func NewLogItem() *LogItem {
	item := new(LogItem)
	item.TS = CurrentTimeMilli()
	item.LogQueue = list.New()

	return item
}

func (l *DeployLog) startClean() {
	l.maintainTimer = time.NewTimer(CLEAN_INTERVAL)
	for {
		select {
		case <-l.maintainTimer.C:
			l.elimExpired()
		}
	}
}

func (l *DeployLog) elimExpired() {
	l.mut.Lock()
	defer l.mut.Unlock()

	for k, v := range l.logMap {
		ts := CurrentTimeMilli()
		if (ts - v.TS) > EXPIRE_TIME {
			delete(l.logMap, k)
		}
	}
}

func (l *DeployLog) PubLog(logKey, log string) {
	if logKey == "" {
		return
	}

	dest := strings.Replace(log, consts.LINE_END, consts.HTML_LINE_END, -1)

	l.mut.Lock()
	defer l.mut.Unlock()

	logItem := l.logMap[logKey]
	if logItem == nil {
		logItem = NewLogItem()
		logItem.PutLog(dest)

		l.logMap[logKey] = logItem
	} else {
		logItem.PutLog(dest)
	}
}

func (l *DeployLog) GetLog(logKey string, result *result.ResultBean) {
	logItem := l.logMap[logKey]
	if logItem == nil {
		result.RET_INFO = ""
		return
	}

	deployLog := logItem.GetLog()
	result.RET_INFO = deployLog
}

func (l *DeployLog) PubSuccessLog(logKey, log string) {
	if logKey == "" {
		return
	}

	str := consts.DEPLOY_SINGLE_SUCCESS_BEGIN_STYLE
	str += log
	str += consts.END_STYLE

	l.PubLog(logKey, str)
}

func (l *DeployLog) PubFailLog(logKey, log string) {
	if logKey == "" {
		return
	}

	str := consts.DEPLOY_SINGLE_FAIL_BEGIN_STYLE
	str += log
	str += consts.END_STYLE

	l.PubLog(logKey, str)
}

func (l *DeployLog) PubErrorLog(logKey, log string) {
	if logKey == "" {
		return
	}

	str := consts.DEPLOY_SINGLE_FAIL_BEGIN_STYLE
	str += log
	str += consts.END_STYLE

	l.PubLog(logKey, str)
}

func (i *LogItem) PutLog(log string) {
	i.mut.Lock()
	defer i.mut.Unlock()

	i.LogQueue.PushBack(log)
	i.TS = CurrentTimeMilli()
}

func (i *LogItem) GetLog() string {
	i.mut.Lock()
	defer i.mut.Unlock()

	var elem *list.Element = i.LogQueue.Front()
	var priv *list.Element = nil

	res := ""
	for {
		if elem == nil {
			break
		}

		priv = elem
		res += priv.Value.(string)
		res += consts.HTML_LINE_END
		elem = elem.Next()

		i.LogQueue.Remove(priv)
	}

	return res
}

func (i *LogItem) Clear() {
	i.mut.Lock()
	defer i.mut.Unlock()

	var elem *list.Element = i.LogQueue.Front()
	var priv *list.Element = nil

	for {
		if elem == nil {
			break
		}

		priv = elem
		elem = elem.Next()

		i.LogQueue.Remove(priv)
	}
}
