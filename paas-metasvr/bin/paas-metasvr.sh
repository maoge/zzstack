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

export LD_LIBRARY_PATH=.:${BASE_DIR}

LIB_DIR=$BASE_DIR/lib
DEPLOY_JARS=`ls $BASE_DIR|grep .jar|awk '{print "'$BASE_DIR'/"$0}'|tr "\n" ":"`
MAIN_CLASS=com.zzstack.paas.underlying.metasvr.startup.Bootstrap
LOGS_DIR=$BASE_DIR/logs
GC_DIR=$LOGS_DIR/gc
CONFIG_DIR=$BASE_DIR/conf
SERVER_NAME=`hostname`

APP_NAME=PAAS-METASVR

if [ ! -d $LOGS_DIR ]; then
    mkdir -p -m 755 $LOGS_DIR
fi

if [ ! -d $GC_DIR ]; then
    mkdir -p -m 755 $GC_DIR
fi

PIDS=`ps -ef | grep java | grep -v grep | grep "$LIB_DIR" |awk '{print $2}'`

ERR_LOG=./logs/stderr.log

#===========================================================================================
# JVM Configuration
#===========================================================================================
JAVA_OPTS=`cat $CONFIG_DIR/jvm.options`
JAVA_OPTS="${JAVA_OPTS} -Xloggc:$GC_DIR/gc_%p.log"
JAVA_OPTS="${JAVA_OPTS} -Duser.home=${BASE_DIR}"
JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext:${LIB_DIR}"
JAVA_OPTS="${JAVA_OPTS} -Ddisplay=pass-metasvr"
JAVA_OPTS="${JAVA_OPTS} -Dlog4j2.formatMsgNoLookups=true"
JAVA_OPTS="${JAVA_OPTS} -cp ${CLASSPATH}:${DEPLOY_JARS}"
JAVA_OPTS="${JAVA_OPTS} ${MAIN_CLASS}"

echo "JAVA_OPTS:"$JAVA_OPTS

function check(){
    if [ -n "$PIDS" ]; then
        echo "ERROR: The ${APP_NAME} already started!"
        echo "PID: $PIDS"
        exit 127
    fi
}

function start(){
    check
    echo "JAVA_HOME:" $JAVA
    echo "Starting the ${APP_NAME} ...\c"

    nohup ${JAVA} ${JAVA_OPTS} "$@" >/dev/null 2>$ERR_LOG &

    sleep 1
    COUNT=`ps -ef | grep java | grep -v grep | grep "$LIB_DIR" | awk '{print $2}' | wc -l`
    if [ $COUNT -gt 0 ]; then
        PIDS=`ps -ef | grep java | grep -v grep | grep "$LIB_DIR" | awk '{print $2}'`
        echo "OK!"
        echo "PID: $PIDS"
        exit 0
    else
        echo "ERROR!"
        echo " The ${APP_NAME} is not start success!!!!"
        exit 127
    fi
}

function stop(){
    if [ -z "$PIDS" ]; then
        echo "ERROR: The ${APP_NAME} does not started!"
    else
        echo -e "Stopping the ${APP_NAME} ...\c"
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
