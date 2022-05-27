#! /bin/bash

PROC_NAME=paas-metasvr-go
ID=ca7310c4-866c-40ca-9464-19d84c40e269
ERR_LOG=./log/stderr.log
PID_FILE=./log/${PROC_NAME}.pid
PWD=`pwd`
PROC_BIN=${PWD}/bin/${PROC_NAME}

function mklogdir(){
    if [ ! -d "./log/" ]; then
        mkdir -p "./log"   
    fi
}

function check(){
    if [ -f "$PID_FILE" ]; then
        pid=`cat $PID_FILE`
        pid_exist=`ps -f -p $pid | grep $PROC_NAME`

        if [ -n "$pid_exist" ]; then
            echo "The $PROC_NAME exists, pid:$pid !"
            exit 127
        fi
    fi

    mklogdir
}

function validate(){
    if [ -f "$PID_FILE" ]; then
        pid=`cat $PID_FILE`
        pid_exist=`ps -f -p $pid | grep $PROC_NAME`
	echo $pid_exist

        if [ -n "$pid_exist" ]; then
            echo "The $PROC_NAME started, pid:$pid"
            exit 0
	else
	    echo "The $PROC_NAME started fail, pid:$pid not exits!"
	    exit 127
        fi
    fi

    echo "ERROR: $PROC_NAME starts fail!"
    exit 127
}

function start(){
    check
    echo "starting $PROC_NAME ..."

    nohup $PROC_BIN "$@" >/dev/null 2>$ERR_LOG &
    echo "$!" > $PID_FILE

    sleep 1
    validate
}

function stop(){
    if [ ! -f "$PID_FILE" ]; then
        echo "$PID_FILE file not exists"
        exit 127
    fi

    pid=`cat $PID_FILE`
    pid_exist=`ps -f -p $pid | grep $PROC_NAME`

    if [ -z "$pid_exist" ]; then
        echo "error: The $PROC_NAME process:$pid does not exists!"
        exit 127
    fi

    echo "stopping $PROC_NAME $pid ......"
    kill $pid > /dev/null 2>&1

    sleep 1
    pid_exist=`ps -f -p $pid | grep $PROC_NAME`
    if [ -n "$pid_exist" ]; then
        kill -9 $pid > /dev/null 2>&1
    fi

    echo "stop $PROC_NAME $pid ok ......"
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