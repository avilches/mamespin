/*
* @author Alberto Vilches
* @date 22/12/2016
*/
package app;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
/**
 * OutputStream filter to emulate a slow device, e.g. modem
 *
 */
public class SlowOutputStream extends FilterOutputStream {
    private final CPSPauser pauser;
    /**
     * Create wrapped Output Stream toe emulate the requested CPS.
     * @param out OutputStream
     * @param pauser characters per second
     */
    public SlowOutputStream(OutputStream out, CPSPauser pauser) {
        super(out);
        this.pauser = pauser;
    }
    // Also handles write(byte[])
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        pauser.pause(len);
        out.write(b, off, len);
    }
    @Override
    public void write(int b) throws IOException {
        pauser.pause(1);
        out.write(b);
    }
}