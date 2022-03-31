package deployutils

import (
	"fmt"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func ExecSimpleCmd(sshClient *SSHClient, cmd, logKey string, paasResult *result.ResultBean) bool {
	_, err := sshClient.GeneralCommand(cmd)
	if err == nil {
		return true
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}
}

func MkDir(sshClient *SSHClient, dir, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -p %s", consts.CMD_MKDIR, dir)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CD(sshClient *SSHClient, dir, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s", consts.CMD_CD, dir)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func IsFileExist(sshClient *SSHClient, fileName string, isDir bool, logKey string, paasResult *result.ResultBean) bool {
	exists, err := sshClient.IsFileExist(fileName, isDir)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if !exists {
		errMsg := fmt.Sprintf("dest file: %s not exits ......", fileName)
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return false
	}

	return true
}

func TAR(sshClient *SSHClient, tarParams, srcFileName, desFileName, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_TAR, tarParams, srcFileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	return IsFileExist(sshClient, desFileName, true, logKey, paasResult)
}

func RM(sshClient *SSHClient, file, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -rf %s", consts.CMD_RM, file)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func MV(sshClient *SSHClient, newName, oldName, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_MV, oldName, newName)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func SED(sshClient *SSHClient, oldValue, newValue, file, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -i 's/%s/%s/g' %s", consts.CMD_SED, oldValue, newValue, file)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func AddLine(sshClient *SSHClient, context, confFile, logKey string, paasResult *result.ResultBean) bool {
	if !ExecSimpleCmd(sshClient, consts.CMD_SETH_PLUS, logKey, paasResult) {
		return false
	}

	cmd := fmt.Sprintf("%s -e \"%s\">>%s\n", consts.CMD_ECHO, context, confFile)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CreateShell(sshClient *SSHClient, fileName, shell, logKey string, paasResult *result.ResultBean) bool {
	if IsFileExist(sshClient, fileName, false, logKey, paasResult) {
		RM(sshClient, fileName, logKey, paasResult)
	}

	if !ExecSimpleCmd(sshClient, consts.CMD_SETH_PLUS, logKey, paasResult) {
		return false
	}

	cmd := fmt.Sprintf("%s -e \"%s\\n\">>%s", consts.CMD_ECHO, consts.SHELL_MACRO, fileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	cmd = fmt.Sprintf("%s -e \"%s\">>%s", consts.CMD_ECHO, shell, fileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	return ChMod(sshClient, fileName, "+x", logKey, paasResult)
}

func ChMod(sshClient *SSHClient, fileName, mod, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_CHMOD, mod, fileName)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CheckPortUp(sshClient *SSHClient, cmpt, instId, servIp, port, logKey string, paasResult *result.ResultBean) bool {
	// ssh2.consumeSurplusBuf();

	isUsed := false
	i := 0
	for {
		if i++; i > consts.CHECK_PORT_RETRY {
			break
		}

		if isUsed, _ = sshClient.IsPortUsed(port); isUsed {
			break
		}

		time.Sleep(time.Duration(100) * time.Millisecond)
	}

	if isUsed {
		info := fmt.Sprintf("deploy %s success, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, servIp, port)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	} else {
		info := fmt.Sprintf("deploy %s fail, inst_id:%s, serv_ip:%s, port:%s startup fail", cmpt, instId, servIp, port)
		global.GLOBAL_RES.PubFailLog(logKey, info)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = info
	}

	return isUsed
}
