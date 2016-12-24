/*
* @author Alberto Vilches
* @date 22/12/2016
*/
package app.download;

public class CPSPauser {

    private final int cps; // Characters per second to emulate
    int total = 0;

    //https://wiki.apache.org/jmeter/Controlling%20Bandwidth%20in%20JMeter%20to%20simulate%20different%20networks
    public static CPSPauser createInKBps(int kbps) {
        return new CPSPauser(kbps * 1024);  // 1 KB/s = 1024B/s
    }

    public static CPSPauser createInKbps(int kbps) {
        return new CPSPauser(kbps * 128);  // 1 Kb/s = 1024b/s = 1024/8 cps = 128
    }

    public static CPSPauser createInMbps(int kbps) {
        return new CPSPauser(kbps * 131072);  // 1 Mb/s = 1024*1024b/s = 1024*1024/8 cps = 131072
    }

    /**
     * Create a pauser with the appropriate speed settings.
     *
     * @param cps CPS to emulate
     */
    public CPSPauser(int cps) {
        if (cps <= 0) {
            throw new IllegalArgumentException("Speed (cps) <= 0");
        }
        this.cps = cps;
    }

    /**
     * Pause for an appropriate time according to the number of bytes being transferred.
     *
     * @param bytes number of bytes being transferred
     */
    public void pause(int bytes) {
        total += bytes;
        long sleepMS = (bytes * MS_PER_SEC) / cps;
        int sleepNS = ((bytes * MS_PER_SEC) / cps) % NS_PER_MS;
//        System.out.println("Writing +"+bytes+"="+total+". Sleeping for "+sleepMS+"."+sleepNS);
        try {
            if (sleepMS > 0 || sleepNS > 0) {
                Thread.sleep(sleepMS, sleepNS);
            }
        } catch (InterruptedException ignored) {
        }
    }

    // Conversions for milli and nano seconds
    private static final int MS_PER_SEC = 1000;
    private static final int NS_PER_SEC = 1000000000;
    private static final int NS_PER_MS = NS_PER_SEC / MS_PER_SEC;
}