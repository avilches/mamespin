/*
* @author Alberto Vilches
* @date 22/12/2016
*/
package app.download;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
/**
 * OutputStream filter to emulate a slow device, e.g. modem
 *
 */
public class SlowOutputStream extends FilterOutputStream {

    static long CHECK_EVERY_MILLIS = 10; // chequeo cada 10 millis

    int cps = -1;
    /**
     * Create wrapped Output Stream toe emulate the requested CPS.
     * @param out OutputStream
     */
    public SlowOutputStream(OutputStream out, int cps) {
        super(out);
        this.cps = cps;
    }

    long timeStart = 0;
    long timeLastCheck = 0;
    long written = 0;
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        if (cps < 0) return; // -1 = no limit
        written += len;
        if (timeStart == 0) {
            timeStart = timeLastCheck = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - timeLastCheck < CHECK_EVERY_MILLIS) return;
        // Chequeamos
        long writtenTimeMillisTheorical = (written * 1000) / cps;
        long writtenTimeMillisReal = now - timeStart;
//        System.out.println("Chequeo "+now+", lastCheck: "+lastCheck+", elapsed check: "+(now - lastCheck )+ "-> total "+total+"*1000 / cps:"+pauser.cps+", millisTheory:"+writtenTimeMillisTheorical+", started: "+start+", writtenTimeMillisReal: "+writtenTimeMillisReal);
        if (writtenTimeMillisTheorical > writtenTimeMillisReal) {
            try {
                System.out.println("    Pause "+(writtenTimeMillisTheorical-writtenTimeMillisReal)+" millis");
                Thread.sleep(writtenTimeMillisTheorical - writtenTimeMillisReal);
            } catch (InterruptedException e) {
            }
        }
        timeLastCheck = now;
    }

}