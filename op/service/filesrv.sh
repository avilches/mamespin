#!/bin/sh

SERVICE_NAME=filesrv
PATH_TO_JAR=/srv/mamespin/bin/filesrv.jar
JVM_PARAMS="-Xms256m -Xmx512m -Djava.awt.headless=true"
APP_PARAMS="/srv/mamespin/config"
LOG_PATH=/srv/mamespin/log/filesrv.log
ERR_PATH=/srv/mamespin/log/filesrv.err

getPid () {
    PID=`ps aux | grep $PATH_TO_JAR | grep -v grep | awk '{print $2}'`
}

start () {
    echo "java $JVM_PARAMS -jar $PATH_TO_JAR $APP_PARAMS 2>> $ERR_PATH >> $LOG_PATH"
          java $JVM_PARAMS -jar $PATH_TO_JAR $APP_PARAMS 2>> $ERR_PATH >> $LOG_PATH &
    status
}

stop () {
    kill $PID
    status
}


status () {
    getPid
    if [ -z $PID ]; then
        echo "$SERVICE_NAME NOT running."
    else
        echo "$SERVICE_NAME running with PID $PID..."
    fi
}

getPid

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ -z $PID ]; then
            start
        else
            echo "$SERVICE_NAME is already running with PID $PID..."
        fi
    ;;
    stop)
        echo "Stopping $SERVICE_NAME ..."
        if [ -z $PID ]; then
            echo "Can't stop $SERVICE_NAME because is not running"
        else
            stop
        fi
    ;;
    restart)
        echo "Restarting $SERVICE_NAME ..."
        if [ -z $PID ]; then
            start
        else
            stop
            start
        fi
    ;;
    status)
        status
    ;;
    *)
        echo "start|stop|restart|status"
    ;;
esac