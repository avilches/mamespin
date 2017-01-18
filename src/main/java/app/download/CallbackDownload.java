/*
* @author Alberto Vilches
* @date 11/01/2017
*/
package app.download;

import app.DbLogic;

import java.util.Date;

public class CallbackDownload {

    public static final int ACCUMULATED_NEEDED_TO_MARK_AS_STARTED = 8192;

    TokenLogic tokenLogic;
    DbLogic.TokenOptions tokenOptions;
    long accumulated = 0;
    long totalSize;
    Date start;

    public CallbackDownload(TokenLogic tokenLogic, DbLogic.TokenOptions tokenOptions, long totalSize) {
        this.tokenLogic = tokenLogic;
        this.tokenOptions = tokenOptions;
        this.totalSize = totalSize;
    }
    double lastPercent = 0;
    // TODO: hacer que no se escriba mas rapido que de 15 segundos o mas lento que de un minuto
    boolean started = false;

    private void start() {
        System.out.println("[start] "+tokenOptions.getId()+" ("+accumulated+"/"+totalSize+")");
        started = true;
        tokenLogic.start(tokenOptions.getId(), start, totalSize);
    }

    private void finish() {
        System.out.println("[finish] "+tokenOptions.getId()+" 100% ("+accumulated+"/"+totalSize+")");
        tokenLogic.finish(tokenOptions.getId(), start, new Date(), accumulated);
    }

    boolean download(long written) {
        if (start == null) {
            start = new Date();
        }
        accumulated += written;
//        System.out.println(written+"/"+accumulated+"/"+totalSize);

        if (accumulated == totalSize) {
            finish();
            return true;
        }

        if (accumulated < ACCUMULATED_NEEDED_TO_MARK_AS_STARTED) {
            return true;
        }

        if (!started) {
            start();
        }

        double percent = ((double)accumulated) / totalSize * 100;
        double diff = percent - lastPercent;
//        System.out.println("Written +"+written+"="+accumulated+", Last percent "+lastPercent+", current percent "+percent+", diff "+diff);
        if (diff >= 1) {
            lastPercent = percent;
            System.out.println("[downloading] "+tokenOptions.getId()+" "+percent+"% ("+accumulated+"/"+totalSize+")");
            return tokenLogic.downloading(accumulated, new Date(), tokenOptions.getId());
        }
        return true;
    }

    void abort() {
        System.out.println("[abort] "+tokenOptions.getId()+" ("+accumulated+"/"+totalSize+")");
        tokenLogic.abort(accumulated, start, new Date(), tokenOptions.getId());
    }

}
