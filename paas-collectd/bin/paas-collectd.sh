#!/bin/bash

error_exit ()
{
    echo "ERROR: $1 !!"
    exit 1
}

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"

cd `dirname $0`
cd ..
BASE_DIR=`pwd`

export JAVA="$JAVA_HOME/bin/java"
export CLASSPATH=.:${CLASSPATH}:${BASE_DIR}/conf

export UUID=%UUID%
export SERV_INST_ID=%SERV_INST_ID%
export META_SVR_URL=%META_SVR_URL%
export META_SVR_USR=%META_SVR_USR%
export META_SVR_PASSWD=%META_SVR_PASSWD%
export COLLECTD_PORT=%COLLECTD_PORT%

export LD_LIBRARY_PATH=.:${BASE_DIR}

LIB_DIR=$BASE_DIR/lib
DEPLOY_JARS=`ls $BASE_DIR|grep .jar|awk '{print "'$BASE_DIR'/"$0}'|tr "\n" ":"`
MAIN_CLASS=com.zzstack.paas.underlying.collectd.Bootstrap
LOGS_DIR=$BASE_DIR/logs
GC_DIR=$LOGS_DIR/gc
CONFIG_DIR=$BASE_DIR/conf
SERVER_NAME=`hostname`

STDOUT_FILE=$LOGS_DIR/stdout.log

if [ ! -d $LOGS_DIR ]; then
    mkdir -p -m 755 $LOGS_DIR
fi

if [ ! -d $GC_DIR ]; then
    mkdir -p -m 755 $GC_DIR
fi

PIDS=`ps -ef | grep java | grep -v grep | grep "$UUID" |awk '{print $2}'`

#===========================================================================================
# JVM Configuration
#===========================================================================================

JAVA_OPTS="${JAVA_OPTS} -server -Xms1g -Xmx1g"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC -XX:G1HeapRegionSize=16m"
JAVA_OPTS="${JAVA_OPTS} -XX:G1ReservePercent=30 -XX:InitiatingHeapOccupancyPercent=25"
JAVA_OPTS="${JAVA_OPTS} -XX:SoftRefLRUPolicyMSPerMB=0 -XX:SurvivorRatio=8"
JAVA_OPTS="${JAVA_OPTS} -verbose:gc -XX:+PrintGCDetails"
JAVA_OPTS="${JAVA_OPTS} -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime"
JAVA_OPTS="${JAVA_OPTS} -XX:+PrintAdaptiveSizePolicy -XX:+UseGCLogFileRotation"
JAVA_OPTS="${JAVA_OPTS} -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
JAVA_OPTS="${JAVA_OPTS} -XX:-OmitStackTraceInFastThrow -XX:+AlwaysPreTouch"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxDirectMemorySize=64m -XX:-UseLargePages -XX:-UseBiasedLocking"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/paas_collectd.dump"
JAVA_OPTS="${JAVA_OPTS} -Xloggc:$GC_DIR/gc_%p.log"
JAVA_OPTS="${JAVA_OPTS} -Duser.home=${BASE_DIR}"
JAVA_OPTS="${JAVA_OPTS} -DUUID=${UUID}"
JAVA_OPTS="${JAVA_OPTS} -DSERV_INST_ID=${SERV_INST_ID}"
JAVA_OPTS="${JAVA_OPTS} -DMETA_SVR_URL=${META_SVR_URL}"
JAVA_OPTS="${JAVA_OPTS} -DMETA_SVR_USR=${META_SVR_USR}"
JAVA_OPTS="${JAVA_OPTS} -DMETA_SVR_PASSWD=${META_SVR_PASSWD}"
JAVA_OPTS="${JAVA_OPTS} -DCOLLECTD_PORT=${COLLECTD_PORT}"
JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${LIB_DIR}"
JAVA_OPTS="${JAVA_OPTS} -Ddisplay=pass-collectd"
JAVA_OPTS="${JAVA_OPTS} -cp ${CLASSPATH}:${DEPLOY_JARS}"
JAVA_OPTS="${JAVA_OPTS} ${MAIN_CLASS}"

echo "JAVA_OPTS:"$JAVA_OPTS

function check(){
    if [ -n "$PIDS" ]; then
        echo "ERROR: The PAAS-COLLECTD already started!"
        echo "PID: $PIDS"
        exit 127
    fi
}

function start(){
    check
    echo "JAVA_HOME:" $JAVA
    echo "Starting the PAAS-COLLECTD ...\c"
    JAVA="java"

    nohup $JAVA $JAVA_OPTS "$@" > $STDOUT_FILE 2>&1 &

    sleep 1
    COUNT=`ps -ef | grep java | grep -v grep | grep "$UUID" | awk '{print $2}' | wc -l`
    if [ $COUNT -gt 0 ]; then
        PIDS=`ps -ef | grep java | grep -v grep | grep "$UUID" | awk '{print $2}'`
        echo "OK!"
        echo "PID: $PIDS"
        echo "STDOUT: $STDOUT_FILE"
        exit 0
    else
        echo "ERROR!"
        echo " The PAAS-COLLECTD is not start success!!!!"
        exit 127
    fi
}

function stop(){
    if [ -z "$PIDS" ]; then
        echo "ERROR: The PAAS-COLLECTD does not started!"
    else
        echo -e "Stopping the PAAS-COLLECTD ...\c"
        for PID in $PIDS ; do
            kill $PID > /dev/null 2>&1
        done
        sleep 2
        for PID in $PIDS ; do
            PID_EXIST=`ps -f -p $PID | grep java`
            if [ -n "$PID_EXIST" ]; then
                kill -9 $PID > /dev/null 2>&1
            fi
        done
        echo "PID: $PIDS"
    fi
}

if [ "$1" = "start" ];then
    start
elif [ "$1" = "stop" ];then 
    stop
elif [ "$1" = "restart" ];then 
    restart
else
    echo " Usage :  $0  [start|stop]"
    echo " start:  start ......"
    echo " stop:   stop ......"
fi

exit 0
