/*
* @author Alberto Vilches
* @date 11/01/2017
*/
package app.download;

import app.DbLogic;

import java.util.Date;

public class CallbackDownload {
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
    int lastPercent = 0;
    // TODO: hacer que no se escriba mas rapido que de 15 segundos o mas lento que de un minuto

    void start() {
        start = new Date();
        tokenLogic.start(tokenOptions.getId(), start, totalSize);
    }

    boolean download(long written) {
        accumulated += written;
        if (accumulated == totalSize) {
            tokenLogic.finish(tokenOptions.getId(), start, new Date(), accumulated);
            return true;
        }
        double percent = ((double)accumulated) / totalSize * 100;
        int diff = ((int)percent) - lastPercent;
//        System.out.println("Written +"+written+"="+accumulated+", Last percent "+lastPercent+", current percent "+percent+", diff "+diff);
        if (diff >= 1) {
//            System.out.println("Written!");
            lastPercent = (int)percent;
            return tokenLogic.downloading(accumulated, new Date(), tokenOptions.getId());
        }
        return true;
    }

    void abort() {
        tokenLogic.abort(accumulated, start, new Date(), tokenOptions.getId());
    }

}
