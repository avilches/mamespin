/*
 * @author Alberto Vilches
 * @date 23/01/2017
 */
package app

class Conf {

    ConfigObject config

    void load(String filename) {
        config = new ConfigSlurper().parse(new File(filename).text)
    }

    int[] loadCps() {
        return config.app.levels.kbs.collect { it > 0 ? it * 1000 : it }.toArray()
    }

}