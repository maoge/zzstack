package deployutils

import (
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func ExecSimpleCmd(sshClient *SSHClient, cmd, logKey string, paasResult *result.ResultBean) bool {
	context, err := sshClient.GeneralCommand(cmd)
	if err == nil {
		return true
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		global.GLOBAL_RES.PubErrorLog(logKey, context)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}
}

func Dos2Unix(sshClient *SSHClient, fileName, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s", consts.CMD_DOS2UNIX, fileName)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func MkDir(sshClient *SSHClient, dir, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -p %s", consts.CMD_MKDIR, dir)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CD(sshClient *SSHClient, dir, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s", consts.CMD_CD, dir)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func PWD(sshClient *SSHClient, logKey string, paasResult *result.ResultBean) (string, error) {
	res, err := sshClient.GeneralCommand(consts.CMD_PWD)
	if err == nil {
		end := strings.Index(res, "\r\n")
		return res[:end], nil
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return "", err
	}
}

func IsDirExistInCurrPath(sshClient *SSHClient, fileDir, logKey string, paasResult *result.ResultBean) (bool, error) {
	cmd := fmt.Sprintf("%s -d %s", consts.CMD_FILE, fileDir)
	context, err := sshClient.GeneralCommand(cmd)
	if err == nil {
		if strings.Index(context, consts.ERR_COMMAND_NOT_FOUND) != -1 {
			errStr := fmt.Sprintf("command %s not found", consts.CMD_FILE)
			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = errStr
			return false, errors.New(errStr)
		}

		res := strings.Index(context, consts.ERR_FILE_DIR_NOT_EXISTS)
		return res != -1, nil
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false, err
	}

	// if (context.contains(CONSTS.COMMAND_NOT_FOUND)) {
	// 	String errInfo = String.format(CONSTS.ERR_COMMAND_NOT_FOUND, CMD_FILE);
	// 	throw new SSHException(errInfo);
	// }

	// // 用户权限不高时不能用下面的方式判断
	// boolean res = context.contains(CONSTS.FILE_DIR_NOT_EXISTS);
}

func GetRedisClusterNode(sshClient *SSHClient, cmd, logKey string, paasResult *result.ResultBean) bool {
	res, err := sshClient.GeneralCommand(consts.CMD_PWD)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	// int first = context.indexOf("@", 0);
	// if (first == -1) return false;

	// int start = context.lastIndexOf("\n", first);
	// int end = context.lastIndexOf("\n");

	// String subStr = context.substring(start + 1, end - 1);
	// result.setRetInfo(subStr);

	// f2a622adffc0dc3f1d83afc27ad2f410ceb5c34d 192.168.238.135:50004@60004 slave 1d4225f3138ae438692f0909d94e5860102b2594 0 1648889988122 4 connected
	// 96b53e439f1ad43051a523c0fbbefc85bff1573a 192.168.238.135:50000@60000 myself,slave ff065678c1b6e5c91aec5f27a94725b4983da715 0 1648889987000 6 connected
	// 84279349bb6c0418855a06bfd99d4499943eba60 192.168.238.135:50001@60001 master - 0 1648889989129 1 connected 0-5460
	// d8a133e016f4350231787a5928572669496a1a21 192.168.238.135:50005@60005 slave 84279349bb6c0418855a06bfd99d4499943eba60 0 1648889989000 5 connected
	// 1d4225f3138ae438692f0909d94e5860102b2594 192.168.238.135:50003@60003 master - 0 1648889990137 3 connected 10923-16383
	// ff065678c1b6e5c91aec5f27a94725b4983da715 192.168.238.135:50002@60002 master - 0 1648889987116 2 connected 5461-10922
	// ultravirs@ubuntu:~/paas/cache/redis/redis_50000$

	first := strings.Index(res, "@")
	if first == -1 {
		return false
	}

	// start := strings.Index()
	end := strings.LastIndex(res, "\r\n")
	subStr := res[0:end]
	paasResult.RET_INFO = subStr
	return true
}

func RemoveRedisNodeFromCluster(sshClient *SSHClient, ip, redisPort, selfId, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("./bin/redis-cli --cluster del-node %s:%s %s -c --no-auth-warning", ip, redisPort, selfId) // // -a passwd
	return sshClient.RemoveFromRedisCluster(cmd, logKey, paasResult)
}

func IsFileExist(sshClient *SSHClient, fileName string, isDir bool, logKey string, paasResult *result.ResultBean) bool {
	exists, err := sshClient.IsFileExist(fileName, isDir)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	return exists
}

func TAR(sshClient *SSHClient, tarParams, srcFileName, desFileName, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_TAR, tarParams, srcFileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	return IsFileExist(sshClient, desFileName, true, logKey, paasResult)
}

func UnZip(sshClient *SSHClient, srcFileName, desFileName, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -o %s", consts.CMD_TAR, srcFileName)
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

func CreateFile(sshClient *SSHClient, fileName, fileContent, logKey string,
	paasResult *result.ResultBean) bool {

	if IsFileExist(sshClient, fileName, false, logKey, paasResult) {
		RM(sshClient, fileName, logKey, paasResult)
	}
	if !ExecSimpleCmd(sshClient, consts.CMD_SETH_PLUS, logKey, paasResult) {
		return false
	}
	idx := strings.LastIndex(fileName, "/")
	cmd := fmt.Sprintf("%s %s", consts.CMD_MKDIR, fileName[:idx])
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}
	cmd = fmt.Sprintf("%s -e \"%s\">>%s", consts.CMD_ECHO, fileContent, fileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}
	return ChMod(sshClient, fileName, "+x", logKey, paasResult)
}

func AddLine(sshClient *SSHClient, context, confFile, logKey string, paasResult *result.ResultBean) bool {
	if !ExecSimpleCmd(sshClient, consts.CMD_SETH_PLUS, logKey, paasResult) {
		return false
	}

	cmd := fmt.Sprintf("%s \"%s\">>%s\n", consts.CMD_ECHO, context, confFile)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func AppendMultiLine(sshClient *SSHClient, oldValue, newValue, file, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s -i '/%s/a\\%s' %s", consts.CMD_SED, oldValue, newValue, file)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	cmd = fmt.Sprintf("%s -i '/%s/d' %s", consts.CMD_SED, oldValue, file)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CreateShell(sshClient *SSHClient, fileName, shell, logKey string, paasResult *result.ResultBean) bool {
	if IsFileExist(sshClient, fileName, false, logKey, paasResult) {
		RM(sshClient, fileName, logKey, paasResult)
	}

	if !ExecSimpleCmd(sshClient, consts.CMD_SETH_PLUS, logKey, paasResult) {
		return false
	}

	cmd := fmt.Sprintf("%s \"%s\\n\">>%s", consts.CMD_ECHO, consts.SHELL_MACRO, fileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	cmd = fmt.Sprintf("%s \"%s\">>%s", consts.CMD_ECHO, shell, fileName)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	return ChMod(sshClient, fileName, "+x", logKey, paasResult)
}

func ChMod(sshClient *SSHClient, fileName, mod, logKey string, paasResult *result.ResultBean) bool {
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_CHMOD, mod, fileName)
	return ExecSimpleCmd(sshClient, cmd, logKey, paasResult)
}

func CheckPortUp(sshClient *SSHClient, cmpt, instId, port, logKey string, paasResult *result.ResultBean) bool {
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
		info := fmt.Sprintf("deploy %s success, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, sshClient.Ip, port)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	} else {
		info := fmt.Sprintf("port:%s check fail", port)
		global.GLOBAL_RES.PubFailLog(logKey, info)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = info
	}

	return isUsed
}

func CheckPortsUp(sshClient *SSHClient, cmpt, instId string, ports []string, logKey string, paasResult *result.ResultBean) bool {
	for _, port := range ports {
		if !CheckPortUp(sshClient, cmpt, instId, port, logKey, paasResult) {
			return false
		}
	}

	return true
}

func CheckPortDown(sshClient *SSHClient, cmpt, instId, port, logKey string, paasResult *result.ResultBean) bool {
	isUsed := false
	i := 0
	for {
		if i++; i > consts.CHECK_PORT_RETRY {
			break
		}
		if isUsed, _ = sshClient.IsPortUsed(port); !isUsed {
			break
		}
		time.Sleep(time.Duration(100) * time.Millisecond)
	}

	if isUsed {
		info := fmt.Sprintf("shutdown %s fail, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, sshClient.Ip, port)
		global.GLOBAL_RES.PubFailLog(logKey, info)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = info
	} else {
		info := fmt.Sprintf("shutdown %s success, inst_id:%s, serv_ip:%s, port:%s", cmpt, instId, sshClient.Ip, port)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	}

	return !isUsed
}
