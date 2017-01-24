/*
 * @author Alberto Vilches
 * @date 23/01/2017
 */
package app

class Conf {

    ConfigObject config
    Map _flatten

    void load(String filename) {
        ConfigSlurper slurper = new ConfigSlurper()
        String text = new File(filename).text
        ConfigObject newConfig = slurper.parse(text)
        config = config == null ? newConfig : config.merge(newConfig)
    }

    int[] loadCps() {
        return config.app.levels.kbs.collect {
            it > 0 ? it * 1000 : it
        }.toArray()
    }

    String[] loadCpsMsgs() {
        return config.app.levels.msg.toArray()
    }


    int getCleanerPeriod() {
        return config.srv.cleaner.schedulerExecuteEveryMinutes
    }

    int getCleanerDelay() {
        return config.srv.cleaner.schedulerDelayToStartMinutes
    }

    int getCleanerTimeout() {
        return config.srv.cleaner.minutesTimeout
    }

    String getVirtualHost() {
        return config.srv.virtualHost
    }

    int getServerPort ( ) {
        return config.srv.serverPort
    }


    Object getDataSourceProperty(String val) {
        def get = flatten().get("dataSource.${val}".toString())
        println "dataSource.${val} = ${get} (${get?.class})"
        return get
    }
    Map flatten() {
        _flatten = (_flatten != null ? _flatten : config.flatten())
    }
}