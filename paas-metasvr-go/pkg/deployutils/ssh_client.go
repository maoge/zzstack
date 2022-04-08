package deployutils

import (
	"bytes"
	"errors"
	"fmt"
	"io"

	"strconv"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
	"golang.org/x/crypto/ssh"
	_ "golang.org/x/crypto/ssh/terminal"
)

var (
	YES_NO                    = []byte("(yes/no)? ")
	YES_NO_FINGERPRINT        = []byte("(yes/no/[fingerprint])? ")
	PASSWD                    = []byte("password: ")
	PASSWD_UPPER              = []byte("Password: ")
	SCP_PASSWD_WRONG          = []byte("Permission denied, please try again.")
	FILE_DIR_NOT_EXISTS       = []byte("No such file or directory")
	REDIS_CLUSTER_INIT_ACCEPT = []byte("(type 'yes' to accept): ")
	REDIS_CLUSTER_INIT_OK     = []byte("[OK] All 16384 slots covered.")
	REDIS_CLUSTER_INIT_ERR    = []byte("ERR")
)

type SSHClient struct {
	Ip         string
	SshPort    int
	User       string
	Passwd     string
	rawClient  *ssh.Client
	rawSession *ssh.Session

	inPipe  io.WriteCloser
	outPipe io.Reader
}

func NewSSHClientBySSH(ssh *proto.PaasSsh) *SSHClient {
	return NewSSHClient(ssh.SERVER_IP, ssh.SSH_PORT, ssh.SSH_NAME, ssh.SSH_PWD)
}

func NewSSHClient(ip string, sshPort int, user string, passwd string) *SSHClient {
	sshClient := new(SSHClient)
	sshClient.Ip = ip
	sshClient.SshPort = sshPort
	sshClient.User = user
	sshClient.Passwd = passwd
	sshClient.rawClient = nil
	sshClient.rawSession = nil

	return sshClient
}

func (h *SSHClient) Connect() bool {
	addr := fmt.Sprintf("%s:%d", h.Ip, h.SshPort)
	client, err := ssh.Dial("tcp", addr, &ssh.ClientConfig{
		User:            h.User,
		Auth:            []ssh.AuthMethod{ssh.Password(h.Passwd)},
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	})

	if err != nil {
		errMsg := fmt.Sprintf("Failed to dial: %s:%d, error:%s", h.Ip, h.SshPort, err.Error())
		utils.LOGGER.Error(errMsg)
		return false
	}

	h.rawClient = client
	h.rawSession, _ = client.NewSession()

	modes := ssh.TerminalModes{
		ssh.ECHO:          0,     // disable echoing
		ssh.TTY_OP_ISPEED: 14400, // input speed = 14.4kbaud
		ssh.TTY_OP_OSPEED: 14400, // output speed = 14.4kbaud
	}

	if err := h.rawSession.RequestPty("xterm", 80, 40, modes); err != nil {
		utils.LOGGER.Error(err.Error())
	}

	h.inPipe, err = h.rawSession.StdinPipe()
	h.outPipe, err = h.rawSession.StdoutPipe()

	if err := h.rawSession.Start("/bin/sh"); err != nil {
		utils.LOGGER.Error(err.Error())
	} else {
		h.ReadToEof() // read and discard shell output
	}

	return true
}

func (h *SSHClient) Close() {
	if h.rawSession != nil {
		h.rawSession.Close()
		h.rawSession = nil
	}
	if h.rawClient != nil {
		h.rawClient.Close()
		h.rawClient = nil
	}
}

func (h *SSHClient) ExecCmd(cmd string) ([]byte, error) {
	buff := fmt.Sprintf("%s\n", cmd)
	size, err := h.inPipe.Write([]byte(buff))
	if err != nil {
		return nil, err
	}
	if size != len(buff) {
		return nil, errors.New("cmd write broken")
	}

	return h.ReadToEof()
}

// 需要交互式执行, 比如scp需要输入密码, 将执行与处理交互式部分分开
func (h *SSHClient) ExecInteractCmd(cmd string) error {
	buff := fmt.Sprintf("%s\n", cmd)
	size, err := h.inPipe.Write([]byte(buff))
	if err != nil {
		return err
	}
	if size != len(buff) {
		return errors.New("cmd write broken")
	}

	return nil
}

func (h *SSHClient) IsFileExist(fileName string, isDir bool) (bool, error) {
	var extendParam string = "-d"
	if isDir {
		extendParam = ""
	}
	cmd := fmt.Sprintf("%s %s %s", consts.CMD_DIR, extendParam, fileName)
	bytes, e := h.ExecCmd(cmd)
	if e != nil {
		return false, e
	} else {
		return isFileExist(bytes), nil
	}
}

func (h *SSHClient) IsPortUsed(port string) (bool, error) {
	cmd := fmt.Sprintf("%s -tnlp | grep LISTEN | awk '{print $4}' | grep :%s$ | wc -l", consts.CMD_NETSTAT, port)
	context, err := h.GeneralCommand(cmd)
	if err != nil {
		return false, err
	}

	arr := strings.Split(context, consts.LINUX_SHELL_SEP)
	len := len(arr)
	count := 0
	if len > 2 {
		str := arr[len-2]
		count, _ = strconv.Atoi(str)
	}

	return (count > 0), nil
}

func (h *SSHClient) GeneralCommand(cmd string) (string, error) {
	if cmd == "" {
		return "", nil
	}

	bytes, err := h.ExecCmd(cmd)
	if err != nil {
		return "", err
	}

	return string(bytes), nil
}

func (h *SSHClient) Dos2Unix(cmd string) (string, error) {
	return h.GeneralCommand(cmd)
}

func (h *SSHClient) SCP(user, passwd, srcHost, sshPort, srcFile, desFile, logKey string,
	paasResult *result.ResultBean) bool {

	cmd := fmt.Sprintf("%s -P %s %s@%s:%s %s", consts.CMD_SCP, sshPort, user, srcHost, srcFile, desFile)
	e := h.ExecInteractCmd(cmd)
	if e != nil {
		errMsg := fmt.Sprintf("scp %s@%s error, %v", user, srcHost, e)
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		return false
	}

	bytes, e := h.ReadScpOut()
	if checkYesOrNo(bytes) || checkYesOrNoFingerPrint(bytes) {
		if e = h.ExecInteractCmd("yes"); e != nil {
			global.GLOBAL_RES.PubErrorLog(logKey, e.Error())

			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = e.Error()

			return false
		}
		bytes, e = h.ReadScpOut()
	}

	if isPasswd(bytes) {
		if e = h.ExecInteractCmd(passwd); e != nil {
			global.GLOBAL_RES.PubErrorLog(logKey, e.Error())

			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = e.Error()

			return false
		}
		bytes, e = h.ReadScpOut()
	}

	if isPasswdWrong(bytes) {
		errMsg := fmt.Sprintf("scp passwd wrong, %s@%s", user, srcHost)
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		return false
	}

	if !isFileExist(bytes) {
		errMsg := fmt.Sprintf(logKey, "scp source file:"+srcFile+" not exists ......")
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		return false
	}

	if eof(bytes) {
		msg := fmt.Sprintf("fetch deploy file: %s, ok ......", srcFile)
		global.GLOBAL_RES.PubLog(logKey, msg)
	}

	return true
}

func (h *SSHClient) InitRedisCluster(cmd string) ([]byte, bool, error) {
	e := h.ExecInteractCmd(cmd)
	if e != nil {
		return nil, false, e
	}

	out := make([]byte, 0)
	bytes, e := h.ReadRedisInitAccept()
	if e != nil {
		return nil, false, e
	}
	out = combine(out, bytes)

	// (type 'yes' to accept):
	if e = h.ExecInteractCmd("yes"); e != nil {
		return nil, false, e
	}
	bytes, e = h.ReadRedisInitOkOrErr()
	if e != nil {
		return nil, false, e
	}
	res := isRedisClusterInitOk(bytes)
	out = combine(out, bytes)

	return out, res, nil
}

func (h *SSHClient) Wait() {
	h.rawSession.Wait()
}

func (h *SSHClient) ReadRedisInitOkOrErr() ([]byte, error) {
	out := make([]byte, 0)
	for {
		tmp := make([]byte, 256)
		size, err := h.outPipe.Read(tmp)
		if err != nil {
			utils.LOGGER.Error(err.Error())
			return nil, err
		}

		if size == 0 {
			break
		} else {
			out = combine(out, trim(tmp))
			if isRedisClusterInitOk(out) || isRedisClusterInitErr(out) {
				break
			}
		}
	}

	return out, nil
}

func (h *SSHClient) ReadRedisInitAccept() ([]byte, error) {
	out := make([]byte, 0)
	for {
		tmp := make([]byte, 256)
		size, err := h.outPipe.Read(tmp)
		if err != nil {
			utils.LOGGER.Error(err.Error())
			return nil, err
		}

		if size == 0 {
			break
		} else {
			out = combine(out, trim(tmp))
			if isAcceptRedisInit(out) {
				break
			}
		}
	}

	return out, nil
}

func (h *SSHClient) ReadScpOut() ([]byte, error) {
	out := make([]byte, 0)
	for {
		tmp := make([]byte, 256)
		size, err := h.outPipe.Read(tmp)
		if err != nil {
			utils.LOGGER.Error(err.Error())
			return nil, err
		}

		if size == 0 {
			break
		} else {
			tmp = trim(tmp)
			combine := [][]byte{out, tmp}
			out = bytes.Join(combine, []byte(""))

			if checkYesOrNo(out) || checkYesOrNoFingerPrint(out) || isPasswd(out) ||
				isPasswdWrong(out) || fileNotExist(out) || eof(out) {

				break
			}
		}
	}

	return out, nil
}

func (h *SSHClient) ReadToEof() ([]byte, error) {
	out := make([]byte, 0)
	for {
		tmp := make([]byte, 256)
		size, err := h.outPipe.Read(tmp)
		if err != nil {
			utils.LOGGER.Error(err.Error())
			return nil, err
		}

		if size == 0 {
			break
		} else {
			tmp = trim(tmp)
			combine := [][]byte{out, tmp}
			out = bytes.Join(combine, []byte(""))

			if eof(out) {
				break
			}
		}

		if err == io.EOF {
			break
		}
	}

	return out, nil
}

func eof(context []byte) bool {
	size := len(context)
	if size < 2 {
		return false
	}

	start := size - 2
	tail := string(context[start:])
	if tail == "$ " || tail == "] " {
		return true
	} else {
		return false
	}
}

func combine(src []byte, append []byte) []byte {
	tmp := [][]byte{src, append}
	return bytes.Join(tmp, []byte(""))
}

func checkYesOrNo(context []byte) bool {
	return bytes.Index(context, YES_NO) != -1
}

func checkYesOrNoFingerPrint(context []byte) bool {
	return bytes.Index(context, YES_NO_FINGERPRINT) != -1
}

func isPasswd(context []byte) bool {
	return bytes.Index(context, PASSWD) != -1 || bytes.Index(context, PASSWD_UPPER) != -1
}

func isPasswdWrong(context []byte) bool {
	return bytes.Index(context, SCP_PASSWD_WRONG) != -1
}

func isFileExist(context []byte) bool {
	return bytes.Index(context, FILE_DIR_NOT_EXISTS) == -1
}

func fileNotExist(context []byte) bool {
	return bytes.Index(context, FILE_DIR_NOT_EXISTS) != -1
}

func isAcceptRedisInit(context []byte) bool {
	return bytes.Index(context, REDIS_CLUSTER_INIT_ACCEPT) != -1
}

func isRedisClusterInitOk(context []byte) bool {
	return bytes.Index(context, REDIS_CLUSTER_INIT_OK) != -1
}

func isRedisClusterInitErr(context []byte) bool {
	return bytes.Index(context, REDIS_CLUSTER_INIT_ERR) != -1
}

func trim(context []byte) []byte {
	idx := bytes.IndexByte(context, '\x00')
	if idx == -1 {
		return context
	} else {
		return context[0:idx]
	}
}

// https://www.php.cn/be/go/476681.html