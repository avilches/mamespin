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
        tokenLogic.markToken(0L, resp.getId(), "download");
    }

    void download(long written) {
        accumulated += written;
        if (accumulated == totalSize) {
            tokenLogic.markToken(resp.getId(), "finished");
            return;
        }
        double percent = ((double)accumulated) / totalSize * 100;
        int diff = ((int)percent) - lastPercent;
//        System.out.println("Written +"+written+"="+accumulated+", Last percent "+lastPercent+", current percent "+percent+", diff "+diff);
        if (diff >= 1) {
//            System.out.println("Written!");
            tokenLogic.markToken(accumulated, resp.getId(), "download");
            lastPercent = (int)percent;
        }
    }

    void abort() {
        // TODO: llevar un registro de abort para evitar abusos
        tokenLogic.markToken(resp.getId(), "unlocked");
    }

}
