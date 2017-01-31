#!/bin/sh

# TODO: falta meter script de arranque, JVM OPTIONS


SERVICE_NAME=filesrv
PATH_TO_JAR=/srv/mamespin/bin/filesrv.jar
PID_PATH_NAME=/srv/mamespin/bin/filesrv.pid
LOG_PATH=/srv/mamespin/log
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            su - avilches nohup java -jar $PATH_TO_JAR /tmp 2>> $LOG_PATH/filesrv.err >> $LOG_PATH/filesrv.out &
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            su - avilches nohup java -jar $PATH_TO_JAR /tmp 2>> $LOG_PATH/filesrv.err >> $LOG_PATH/filesrv.out &
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac
