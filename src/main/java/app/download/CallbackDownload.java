/*
* @author Alberto Vilches
* @date 11/01/2017
*/
package app.download;

import app.DbLogic;

public class CallbackDownload {
    TokenLogic tokenLogic;
    DbLogic.TokenOptions resp;
    long accumulated = 0;
    long totalSize;

    public CallbackDownload(TokenLogic tokenLogic, DbLogic.TokenOptions resp, long totalSize) {
        this.tokenLogic = tokenLogic;
        this.resp = resp;
        this.totalSize = totalSize;
    }
    int lastPercent = 0;
    // TODO: hacer que no se escriba mas rapido que de 15 segundos o mas lento que de un minuto

    void start() {
        tokenLogic.start(resp.getId(), totalSize);
    }

    boolean download(long written) {
        accumulated += written;
        if (accumulated == totalSize) {
            tokenLogic.finish(resp.getId(), accumulated);
            return true;
        }
        double percent = ((double)accumulated) / totalSize * 100;
        int diff = ((int)percent) - lastPercent;
//        System.out.println("Written +"+written+"="+accumulated+", Last percent "+lastPercent+", current percent "+percent+", diff "+diff);
        if (diff >= 1) {
//            System.out.println("Written!");
            lastPercent = (int)percent;
            return tokenLogic.downloading(accumulated, resp.getId());
        }
        return true;
    }

    void abort() {
        tokenLogic.abort(accumulated, resp.getId());
    }

}
