package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.exception.SSHException;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.utils.StringUtils;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class SSHExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SSHExecutor.class);

    private static int CONN_TIMEOUT = 20000;
    private static long WAIT_PORT_TIMEOUT = 100000L;

    public static final String CMD_SETH_PLUS = "set +H";
    public static final String CMD_TAIL = "tail";
    public static final String CMD_CD = "cd";
    public static final String CMD_PWD = "pwd";
    public static final String CMD_CP = "cp";
    public static final String CMD_SCP = "scp";
    public static final String CMD_FILE = "file";
    public static final String CMD_MKDIR = "mkdir";
    public static final String CMD_TAR = "tar";
    public static final String CMD_UNZIP = "unzip";
    public static final String CMD_EXPORT = "export";
    public static final String CMD_CAT = "cat";
    public static final String CMD_TOUCH = "touch";
    public static final String CMD_CAT_END = "EOF";
    public static final String CMD_SOURCE = "source";
    public static final String CMD_NETSTAT = "netstat";
    public static final String CMD_ECHO = "echo";
    public static final String CMD_MV = "mv";
    public static final String CMD_RM = "rm";
    public static final String CMD_CHMOD = "chmod";
    public static final String CMD_HOSTNAME = "hostname";
    public static final String CMD_HNAME2IP = "hname2ip";
    public static final String CMD_ERL = "erl";
    public static final String CMD_SED = "sed";
    public static final String CMD_DIR = "dir";
    public static final String CMD_SET_PS1 = "export PS1=\"[\\u@\\h \\W]\\$ \"";
    public static final String CMD_DOS2UNIX = "dos2unix";
    public static final String CMD_PS = "ps";
    
    public static final String SHELL_UNTERMINATED = "unterminated";

    public static final String END_DALLAR_BLANK = "$ ";
    public static final String END_MORE_BLANK = "> ";
    public static final String END_BRACKET_DALLAR = "]$";

    public static final byte[] YES_NO = { '(', 'y', 'e', 's', '/', 'n', 'o', ')', '?', ' ' };
    public static final byte[] YES_NO_FINGERPRINT = { '(', 'y', 'e', 's', '/', 'n', 'o', '/', '[', 'f', 'i', 'n', 'g', 'e', 'r', 'p', 'r', 'i', 'n', 't', ']', ')', '?', ' ' };
    public static final byte[] PASSWD = { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ':', ' ' };

    public static final String REDIS_CLUSTER_INIT_ACCEPT = "(type 'yes' to accept): ";
    public static final String REDIS_CLUSTER_INIT_OK = "[OK] All 16384 slots covered.";
    public static final String REDIS_CLUSTER_INIT_ERR = "ERR";
    public static final String REDIS_CLUSTER_DELETE_NODE = ">>> Sending CLUSTER RESET SOFT to the deleted node.";
    public static final String REDIS_ADD_NODE_OK = "[OK] New node added correctly.";
    public static final String REDIS_SLAVEOF_OK = "OK";
    public static final String REDIS_MOVEING_SLOT = "Moving slot";
    public static final String COMMON_JDK_OK = "openjdk version \"1.8.0_181\"";
    public static final String TAOS_CMD_END = "taos> ";
    public static final String TAOS_SHELL_END = "Query OK";
    public static final String TAOS_DNODE_READY = "ready";
    public static final String TAOS_DNODE_DROPPING = "dropping";
    public static final String VOLTDB_CONN_FAIL = "Unable to connect to VoltDB cluster";
    public static final String FILE_DIR_NOT_EXISTS = "No such file or directory";
    public static final String SCP_PASSWD_WRONG = "Permission denied, please try again.";

    private JschUserInfo ui;
    private JSch jsch;
    private Session session;
    private ChannelShell channel;

    private InputStream in;
    private OutputStream os;

    private final int BUF_LEN = 1024;
    private byte[] buf;

    private static final long CPU_SLICE = 50L;
    public static final boolean PRINT_LOG = false;

    private volatile boolean isLogined = false;

    public SSHExecutor(String user, String passwd, String host, int sshPort) {
        this.ui = new JschUserInfo(user, passwd, host, sshPort);
    }

    public SSHExecutor(PaasSsh paasSSH) {
        this.ui = new JschUserInfo(paasSSH.getSshName(), paasSSH.getSshPwd(), paasSSH.getServerIp(),
                paasSSH.getSshPort());
    }

    public boolean login(String logKey, ResultBean result) {
        if (isLogined)
            return true;

        try {
            buf = new byte[BUF_LEN];

            jsch = new JSch();
            session = jsch.getSession(ui.getUser(), ui.getHost(), ui.getSshPort());
            session.setUserInfo(ui);

            // 防止远程主机GSSAPIAuthentication=yes 导致j2sh无法连接
            session.setConfig("userauth.gssapi-with-mic", "no");
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect(CONN_TIMEOUT);
            channel = (ChannelShell) session.openChannel("shell");
            channel.connect(CONN_TIMEOUT);

            in = channel.getInputStream();
            os = channel.getOutputStream();

            // TimeUnit.MILLISECONDS.sleep(100L);

            consumeSurplusBuf();

            // set PS1 to surpport format
            generalCommand(CMD_SET_PS1);
            isLogined = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("host info:{}", ui.toString());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return isLogined;
    }

    public void close() {
        try {
            if (os != null) {
                os.close();
                os = null;
            }

            if (in != null) {
                in.close();
                in = null;
            }

            if (channel != null) {
                channel.disconnect();
                channel = null;
            }

            if (session != null) {
                session.disconnect();
                session = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void writeLn(String command) throws SSHException {
        try {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            // os.write(command.getBytes(), Charset.);
            os.write(bytes, 0, bytes.length);

            os.write("\n".getBytes());
            os.flush();
        } catch (IOException e) {
            throw new SSHException(e.getMessage());
        }
    }

    // end with: "$ " or "> " or "]$ "
    private boolean isSshEof(String context) {
        int len = context.length();
        if (len < 2)
            return false;

        return (context.charAt(len - 2) == 36 && context.charAt(len - 1) == 32)
              || (context.charAt(len - 2) == 62 && context.charAt(len - 1) == 32)
              || (context.charAt(len - 2) == 93 && context.charAt(len - 1) == 36);
    }
    
    private boolean scpFileNotExists(String context) {
        return context.indexOf(FILE_DIR_NOT_EXISTS) != -1;
    }
    
    private boolean isPasswdWrong(String context) {
        return context.indexOf(SCP_PASSWD_WRONG) != -1;
    }

    // SSH Are you sure you want to continue connecting (yes/no)?
    private boolean checkYesOrNo(String context) {
        int len = YES_NO.length;
        int count = context.length();

        if (count < len)
            return false;

        boolean equal = true;
        for (int i = 0; i < len; i++) {
            if (YES_NO[i] != context.charAt(count - len + i)) {
                equal = false;
                break;
            }
        }

        return equal;
    }
    
    private boolean checkYesOrNoFingerPrint(String context) {
        int len = YES_NO_FINGERPRINT.length;
        int count = context.length();

        if (count < len)
            return false;

        boolean equal = true;
        for (int i = 0; i < len; i++) {
            if (YES_NO_FINGERPRINT[i] != context.charAt(count - len + i)) {
                equal = false;
                break;
            }
        }

        return equal;
    }
    
    // end with: "> "
    private boolean isVoltCmdEof(String context) {
        int len = context.length();
        if (len < 2)
            return false;

        return (context.charAt(len - 2) == 62 && context.charAt(len - 1) == 32);
    }

    private boolean isPasswd(String context) {
        int len = PASSWD.length;
        int count = context.length();

        if (count < len)
            return false;

        boolean equal = true;

        for (int i = 0; i < len; i++) {
            if (i == 0) {
                // in some version of linux, it starts with: 'P'
                if (Character.toUpperCase(PASSWD[i]) != Character.toUpperCase(context.charAt(count - len + i))) {
                    equal = false;
                    break;
                }
            } else {
                if (PASSWD[i] != context.charAt(count - len + i)) {
                    equal = false;
                    break;
                }
            }
        }

        return equal;
    }

    // redis cluster init: "(type 'yes' to accept):"
    public boolean isAcceptRedisInit(String context) {
        int len = REDIS_CLUSTER_INIT_ACCEPT.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_CLUSTER_INIT_ACCEPT);
    }

    public boolean isTaosShellEnd(String context) {
        int len = TAOS_CMD_END.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(TAOS_CMD_END);
    }

    public boolean isTaosShowDNodeEnd(String context) {
        int len = TAOS_SHELL_END.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(TAOS_SHELL_END);
    }

    public boolean isRedisClusterInitOk(String context) {
        int len = REDIS_CLUSTER_INIT_OK.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_CLUSTER_INIT_OK);
    }

    public boolean isRedisClusterInitErr(String context) {
        int len = REDIS_CLUSTER_INIT_ERR.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_CLUSTER_INIT_ERR);
    }

    public boolean isRedisSlaveOfOk(String context) {
        int len = REDIS_SLAVEOF_OK.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_SLAVEOF_OK);
    }

    public boolean isJdkOk(String context) {
        int len = COMMON_JDK_OK.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.indexOf(COMMON_JDK_OK) != -1;
    }

    public boolean isRedisAddNodeOk(String context) {
        int len = REDIS_ADD_NODE_OK.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_ADD_NODE_OK);
    }

    public boolean isRedisClusterDeleteNode(String context) {
        int len = REDIS_CLUSTER_DELETE_NODE.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_CLUSTER_DELETE_NODE);
    }

    // REDIS_MOVEING_SLOT
    public boolean isRedisMoveingSlot(String context) {
        int len = REDIS_MOVEING_SLOT.length();
        int count = context.length();

        if (count < len)
            return false;

        return context.contains(REDIS_MOVEING_SLOT);
    }

    public void consumeSurplusBuf() {
        long start = System.currentTimeMillis();
        String context = "";

        try {
            do {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);

                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.READ_BUF_TIMEOUT)
                    break;

            } while (!isSshEof(context));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public String generalCommand(String cmd) throws SSHException {
        if (StringUtils.isNull(cmd))
            return "";

        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        try {
            do {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);

                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } while (!isSshEof(context));
        } catch (Exception e) {
            logger.error("exec cmd:{}, error:{}", cmd, e.getMessage());
            throw new SSHException(e.getMessage());
        }

        return context.toString();
    }

    public boolean execSimpleCmd(String cmd, String logKey, boolean print) throws SSHException {
        boolean res = false;
        try {
            String context = generalCommand(cmd);
            if (print && !StringUtils.isNull(logKey))
                DeployLog.pubLog(logKey, context);

            res = context.indexOf(SHELL_UNTERMINATED) == -1;
        } catch (SSHException e) {
            throw e;
        }

        return res;
    }

    public String tail(String file, int lines, String logKey) throws SSHException {
        String cmd = String.format("%s -n %d %s", CMD_TAIL, (lines > 0 ? lines : 100), file);
        return generalCommand(cmd);
    }

    public boolean cd(String path, String logKey) throws SSHException {
        String cmd = String.format("%s %s", CMD_CD, path);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean cdHome(String logKey) throws SSHException {
        return cd("$HOME", logKey);
    }

    public String pwd() throws SSHException {
        String result = generalCommand(CMD_PWD);
        String[] arr = result.split(CONSTS.LINUX_SHELL_SEP);
        return arr.length > 1 ? arr[1] : "";
    }

    public String getHome() throws SSHException {
        String cmd = String.format("%s %s", CMD_ECHO, "$HOME");
        String result = generalCommand(cmd);
        String[] arr = result.split(CONSTS.LINUX_SHELL_SEP);
        return arr.length > 1 ? arr[1] : "";
    }

    public String getHostname() throws SSHException {
        String cmd = String.format("%s", CMD_HOSTNAME);
        String result = generalCommand(cmd);
        String[] arr = result.split(CONSTS.LINUX_SHELL_SEP);
        return arr.length > 1 ? arr[1] : "";
    }

    public boolean cp(String srcFile, String destDir, String logKey) throws SSHException {
        String cmd = String.format("%s %s %s", CMD_CP, srcFile, destDir);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean mv(String oldFile, String newFile) throws SSHException {
        String cmd = String.format("%s %s %s", CMD_MV, oldFile, newFile);
        String context = generalCommand(cmd);
        return context.contains(CONSTS.NO_SUCH_FILE);
    }

    public void rm(String file, boolean recursive, String logKey) throws SSHException {
        String extend = recursive ? "-rf" : "";
        String cmd = String.format("%s %s %s", CMD_RM, extend, file);
        generalCommand(cmd);
    }

    public boolean isFileExist(String file, boolean isDir, String logKey) throws SSHException {
        String extendParam = isDir ? "-d" : "";
        String cmd = String.format("%s %s %s", CMD_DIR, extendParam, file);
        String context = generalCommand(cmd);

        boolean res = context.contains(CONSTS.FILE_DIR_NOT_EXISTS);
        return !res;
    }

    public boolean isDirExistInCurrPath(String fileDir, String logKey) throws SSHException {
        String cmd = String.format("%s -d %s", CMD_FILE, fileDir);
        String context = generalCommand(cmd);

        if (context.contains(CONSTS.COMMAND_NOT_FOUND)) {
            String errInfo = String.format(CONSTS.ERR_COMMAND_NOT_FOUND, CMD_FILE);
            throw new SSHException(errInfo);
        }

        // 用户权限不高时不能用下面的方式判断
        boolean res = context.contains(CONSTS.FILE_DIR_NOT_EXISTS);
        return !res;
    }

    public boolean mkdir(String fileDir, String logKey) throws SSHException {
        String cmd = String.format("%s -p %s", CMD_MKDIR, fileDir);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    // sed -i "s/%JDK_ROOT_PATH%/home/g" access.sh
    public boolean sed(String oldValue, String newValue, String fileName, String logKey) throws SSHException {
        String cmd = String.format("%s -i 's/%s/%s/g' %s", CMD_SED, oldValue, newValue, fileName);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean rmLine(String s, String fileName, String logKey) throws SSHException {
        String cmd = String.format("%s -i '/^%s/d' %s %s", CMD_SED, s, fileName, CONSTS.LINE_SEP);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean addLine(String s, String fileName, String logKey) throws SSHException {
        // sed -i '$a aaa' test.txt
        String cmd = String.format("%s -i '$a %s' %s %s", CMD_SED, s, fileName, CONSTS.LINE_SEP);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean initRedisCluster(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isAcceptRedisInit(context));

        writeLn("yes");
        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isRedisClusterInitOk(context) && !isRedisClusterInitErr(context));

        boolean res = isRedisClusterInitOk(context);
        if (!StringUtils.isNull(logKey))
            DeployLog.pubLog(logKey, context);

        return res;
    }

    public boolean loginTaosShell(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isTaosShellEnd(context));

        return true;
    }

    public boolean createOrDropNode(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isTaosShowDNodeEnd(context));

        return isTaosShowDNodeEnd(context);
    }

    public boolean checkNodeOnline(String cmd, String node, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isTaosShowDNodeEnd(context));

        // id      |           end_point            | vnodes | cores  |   status   | role  |       create_time       |      offline reason      |
        // ======================================================================================================================================
        //       1 | 172.20.0.171:3001              |      1 |     56 | ready      | any   | 2021-03-10 15:40:45.600 |                          |
        //       2 | 172.20.0.172:3001              |      0 |     56 | ready      | any   | 2021-03-10 15:48:30.122 |                          |
        //       0 | 172.20.0.171:3000              |      0 |      0 | ready      | arb   | 1970-01-01 08:00:00.000 | -                        |

        String[] arr = context.split("\n");
        boolean res = false;
        for (String line : arr) {
            int startIdx = line.indexOf(node);
            if (startIdx == -1)
                continue;

            String[] cols = line.split("\\|");
            String status = cols[4].trim();
            res = status.equals(TAOS_DNODE_READY);
        }

        return res;
    }

    public boolean checkNodeOffLine(String cmd, String node, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isTaosShowDNodeEnd(context));

        // id      |           end_point            | vnodes | cores  |   status   | role  |       create_time       |      offline reason      |
        // ======================================================================================================================================
        //       1 | 172.20.0.171:3001              |      1 |     56 | ready      | any   | 2021-03-10 15:40:45.600 |                          |
        //       2 | 172.20.0.172:3001              |      0 |     56 | ready      | any   | 2021-03-10 15:48:30.122 |                          |
        //       0 | 172.20.0.171:3000              |      0 |      0 | dropping   | arb   | 1970-01-01 08:00:00.000 | -                        |

        return isTaosShowDNodeEnd(context);
    }

    public boolean joinRedisCluster(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isRedisAddNodeOk(context) && !isRedisClusterInitErr(context));

        return isRedisAddNodeOk(context);
    }

    public boolean removeFromRedisCluster(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isRedisClusterInitErr(context) && !isRedisClusterDeleteNode(context));

        return isRedisClusterDeleteNode(context);
    }

    public boolean migrateRedisClusterSlot(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isRedisClusterInitErr(context) && !isSshEof(context));

        if (isRedisClusterInitErr(context)) return false;

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isSshEof(context));

        return isRedisMoveingSlot(context);
    }

    public boolean redisSlaveof(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isSshEof(context));

        return isRedisSlaveOfOk(context);
    }

    public boolean isExistedJdk(String cmd, String logKey) throws SSHException {
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = new String();

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isSshEof(context));

        return isJdkOk(context);
    }

    public boolean scp(String user, String passwd, String srcHost, String srcFile, String desFile, String sshPort,
            String logKey) throws SSHException {
        String cmd = String.format("%s -P %s %s@%s:%s %s", CMD_SCP, sshPort, user, srcHost, srcFile, desFile);
        writeLn(cmd);

        long start = System.currentTimeMillis();
        String context = "";

        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                long curr = System.currentTimeMillis();
                if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!checkYesOrNo(context) && !checkYesOrNoFingerPrint(context) && !isPasswd(context));

        if (checkYesOrNo(context) || checkYesOrNoFingerPrint(context)) {
            writeLn("yes");

            do {
                try {
                    int avail = in.available();
                    if (avail > 0) {
                        int len = in.read(buf, 0, BUF_LEN);
                        if (len < 0)
                            break;
                        else if (len > 0) {
                            String s = new String(buf, 0, len);
                            context += s;
                        }
                    }

                    TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                    long curr = System.currentTimeMillis();
                    if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                        throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

                } catch (Exception e) {
                    throw new SSHException(e);
                }

            } while (!isPasswd(context));
        }

        if (isPasswd(context)) {
            writeLn(passwd);
            do {
                try {
                    int avail = in.available();
                    if (avail > 0) {
                        int len = in.read(buf, 0, BUF_LEN);
                        if (len < 0)
                            break;
                        else if (len > 0) {
                            String s = new String(buf, 0, len);
                            context += s;
                        }
                    }

                    if (isPasswdWrong(context)) {
                        String info = String.format("scp passwd wrong, %s@%s", user, srcHost);
                        DeployLog.pubErrorLog(logKey, info);
                        return false;
                    }
                    
                    TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                    long curr = System.currentTimeMillis();
                    if ((curr - start) > CONSTS.SSH_CMD_TIMEOUT)
                        throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);

                } catch (Exception e) {
                    throw new SSHException(e);
                }

            } while (!isSshEof(context));
        }
        
        if (scpFileNotExists(context)) {
            DeployLog.pubErrorLog(logKey, "scp source file:" + srcFile + " not exists ......");
            return false;
        }

        return true;
    }
    
    public boolean createValidationTable(String sqlCmd, String logKey) throws SSHException {
        do {
            writeLn(sqlCmd);
        } while (!waitVoltCmdEof(3000));
        
        do {
            writeLn("create table dual (id varchar(48) not null primary key);");
        } while (!waitVoltCmdEof(3000));
        
        do {
            writeLn("insert into dual values('abc');");
        } while (!waitVoltCmdEof(3000));

        writeLn("exit");
        return true;
    }
    
    private boolean waitVoltCmdEof(int timeout) throws SSHException {
        long start = System.currentTimeMillis();
        String context = "";
        
        do {
            try {
                int avail = in.available();
                if (avail > 0) {
                    int len = in.read(buf, 0, BUF_LEN);
                    if (len < 0)
                        break;
                    else if (len > 0) {
                        String s = new String(buf, 0, len);
                        context += s;
                    }
                }

                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);
                if (context.contains(VOLTDB_CONN_FAIL)) {
                    return false;
                }
                
                long curr = System.currentTimeMillis();
                if ((curr - start) > timeout) {
                    throw new SSHException(CONSTS.ERR_SSH_TIMEOUT);
                }

            } catch (Exception e) {
                throw new SSHException(e);
            }
        } while (!isVoltCmdEof(context));
        
        return true;
    }

    public boolean tgzUnpack(String fileName, String logKey) throws SSHException {
        String cmd = String.format("%s -zxvf %s", CMD_TAR, fileName);
        return execSimpleCmd(cmd, null, PRINT_LOG);
    }

    public boolean checkHostExist(String hName) throws SSHException {
        String cmd = String.format("%s -n %s", CMD_HNAME2IP, hName);
        String context = generalCommand(cmd);
        return !context.contains(CONSTS.NO_MAPPING_IN_HOSTS);
    }

    public boolean validateEnv() throws SSHException {
        String cmd = String.format("%s %s", CMD_SOURCE, "$HOME/" + CONSTS.BASH_PROFILE);
        return execSimpleCmd(cmd, null, PRINT_LOG);
    }

    public boolean addExecMod(String file) throws SSHException {
        String cmd = String.format("%s +x %s", CMD_CHMOD, file);
        return execSimpleCmd(cmd, null, PRINT_LOG);
    }

    public boolean chmod(String file, String mode) throws SSHException {
        String cmd = String.format("%s %s %s", CMD_CHMOD, mode, file);
        return execSimpleCmd(cmd, null, PRINT_LOG);
    }

    public boolean createStartShell(String shellContext) throws SSHException {
        return createShell(shellContext, FixDefs.START_SHELL);
    }

    public boolean createStopShell(String shellContext) throws SSHException {
        return createShell(shellContext, FixDefs.STOP_SHELL);
    }

    private boolean createShell(String shellContext, String shell) throws SSHException {
        // if shell exists, delete it
        rm(shell, false, null);

        String cmd = String.format("%s", CMD_SETH_PLUS);
        if (execSimpleCmd(cmd, null, PRINT_LOG)) return false;

        cmd = String.format("%s -e \"%s\">>%s", CMD_ECHO, CONSTS.SHELL_MACRO, shell);
        if (execSimpleCmd(cmd, null, PRINT_LOG)) return false;

        // new line
        cmd = String.format("%s -e \"\\\n\">>%s\n", CMD_ECHO, shell);
        if (execSimpleCmd(cmd, null, PRINT_LOG)) return false;

        cmd = String.format("%s -e \"%s\">>%s", CMD_ECHO, shellContext, shell);
        if (execSimpleCmd(cmd, null, PRINT_LOG)) return false;

        return addExecMod(shell);
    }

    public String readStartShell() throws SSHException {
        return readShell(FixDefs.START_SHELL);
    }

    public String readStopShell(String shellContext) throws SSHException {
        return readShell(FixDefs.STOP_SHELL);
    }

    private String readShell(String shell) throws SSHException {
        String cmd = String.format("%s %s", CMD_CAT, shell);
        String result = generalCommand(cmd);

        return result.substring(result.indexOf("\r\n") + 2, result.lastIndexOf("\r\n"));
    }

    public boolean execStartShell(String logKey) throws SSHException {
        return execShell(FixDefs.START_SHELL, logKey);
    }

    public boolean execStopShell(String logKey) throws SSHException {
        return execShell(FixDefs.STOP_SHELL, logKey);
    }

    private boolean execShell(String shell, String logKey) throws SSHException {
        String cmd = String.format("./%s", shell);
        return execSimpleCmd(cmd, logKey, PRINT_LOG);
    }

    public boolean isCmdValid(String command, String logKey) throws SSHException {
        String cmd = String.format("%s --version", command);
        String context = generalCommand(cmd);

        return !context.contains(CONSTS.COMMAND_NOT_FOUND);
    }

    public void echo(String text) throws SSHException {
        String cmd = String.format("%s %s", CMD_ECHO, text);
        generalCommand(cmd);
    }

    public boolean isPortUsed(String port, String logKey) throws SSHException {
        String cmd = String.format("%s -tnlp | grep LISTEN | awk '{print $4}' | grep :%s$ | wc -l", CMD_NETSTAT, port);
        String context = generalCommand(cmd);

        String[] arr = context.split(CONSTS.LINUX_SHELL_SEP);
        int len = arr.length;
        String result = len > 2 ? arr[len - 2] : "0";

        return Integer.valueOf(result) > 0;
    }

    public boolean isProcExist(String identify, String logKey) throws SSHException {
        String cmd = String.format("%s -ef | grep %s | grep -v grep | wc -l", CMD_PS, identify);
        String context = generalCommand(cmd);

        String[] arr = context.split(CONSTS.LINUX_SHELL_SEP);
        int len = arr.length;
        String result = len > 2 ? arr[len - 2] : "0";

        return Integer.valueOf(result) > 0;
    }

    public boolean waitProcessStart(String port, String logKey) {
        return waitProcessStart(port, logKey, null);
    }

    public boolean waitProcessStart(String port, String logKey, ResultBean result) {
        boolean ret = true;
        try {
            long beginTs = System.currentTimeMillis();
            long currTs = beginTs;

            do {
                TimeUnit.MILLISECONDS.sleep(CPU_SLICE);

                currTs = System.currentTimeMillis();
                if ((currTs - beginTs) > CONSTS.SSH_CMD_TIMEOUT) {
                    ret = false;
                    if (result != null) {
                        result.setRetCode(CONSTS.REVOKE_NOK);
                        result.setRetInfo("Start process timeout...");
                    }
                    break;
                }
                echo("......");

            } while (!isPortUsed(port, logKey));

        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    public boolean waitProcessStop(String port, String logKey) {
        boolean ret = true;
        try {
            long beginTs = System.currentTimeMillis();
            long currTs = beginTs;

            do {
                Thread.sleep(CONSTS.DEPLOY_CHECK_INTERVAL);
                currTs = System.currentTimeMillis();
                if ((currTs - beginTs) > WAIT_PORT_TIMEOUT) {
                    ret = false;
                    break;
                }

                echo("......");

            } while (isPortUsed(port, logKey));

        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    public boolean dos2unix(String cmd, String logKey, ResultBean result) throws SSHException {
        boolean ret = true;
        String strResult = generalCommand(cmd);
        if (strResult.indexOf(CONSTS.COMMAND_NOT_FOUND) != -1) {
            ret = false;

            String errInfo = String.format("command: %s not found", CMD_DOS2UNIX);
            DeployLog.pubErrorLog(logKey, errInfo);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(errInfo);
        }

        return ret;
    }


    public boolean unzip(String cmd, String logKey, ResultBean result) throws SSHException {
        boolean ret = true;
        String strResult = generalCommand(cmd);
        if (strResult.indexOf(CONSTS.COMMAND_NOT_FOUND) != -1) {
            ret = false;

            String errInfo = String.format("command: %s not found", CMD_UNZIP);
            DeployLog.pubErrorLog(logKey, errInfo);

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(errInfo);
        }

        return ret;
    }


    /*public boolean checkNodeOnline(String firstDnodeIp, String firstDnodePort, String nodeIp, String logKey) throws SSHException {
        String export = "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib";
        generalCommand(export);

        String taosShellCmd = String.format("./bin/taos -h %s -P %s -c ./etc",firstDnodeIp,firstDnodePort);
        loginTaosShell(taosShellCmd, null);

        String showNodes = "show dnodes;";
        return checkNodeOnline(showNodes, nodeIp, null);
    }

    public static void main(String[] args) throws SSHException {
        SSHExecutor exec = new SSHExecutor("sms", "wlwx2021", "172.20.0.172", 22);
        exec.login(null, null);

        try {
            exec.pwd();
            exec.cd("paas/db/tdengine-2.0.16_3001", null);
            exec.pwd();

            exec.checkNodeOnline("172.20.0.171", "3001", "172.20.0.172:3001", null);

            exec.close();
        } catch (Exception e) {
            ;
        }
    }*/

}
