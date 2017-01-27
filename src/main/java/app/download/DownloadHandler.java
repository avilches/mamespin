/*
* @author Alberto Vilches
* @date 11/01/2017
*/
package app.download;

import app.DbLogic;
import org.eclipse.jetty.server.Server;

import java.util.Date;

public class DownloadHandler {

    // Wait for the JDownloader GET chopped instead of HEAD
    public static final int ACCUMULATED_NEEDED_TO_MARK_AS_STARTED = 8192;
    public static final int UPDATE_EVERY_MILLIS_MIN = 15000;
    public static final int UPDATE_EVERY_MILLIS_MAX = 30000;

    TokenLogic tokenLogic;
    DbLogic.TokenOptions tokenOptions;
    long totalSize;
    Date start;

    Server server;

    public DownloadHandler(Server server, TokenLogic tokenLogic, DbLogic.TokenOptions tokenOptions, long totalSize) {
        this.server = server;
        this.tokenLogic = tokenLogic;
        this.tokenOptions = tokenOptions;
        this.totalSize = totalSize;
    }

    private double lastPercent = 0;
    private double lastCheck = 0;
    private boolean started = false;

    long totalWritten = 0;

    boolean download(long written) {
        if (!server.isRunning()) {
            System.out.println("[shutdown] "+tokenOptions.getId()+" ("+ totalWritten +"/"+totalSize+")!!!!");
            return false;
        }
        if (start == null) {
            start = new Date();
        }
        totalWritten += written;
//        System.out.println(written+"/"+accumulated+"/"+totalSize);

        if (totalWritten == totalSize) {
            // En ficheros muy pequeños es posible acabar y no haber llegado a marcarlo como start o download
            System.out.println("[finish] "+tokenOptions.getId()+" 100% ("+ totalWritten +"/"+totalSize+")");
            tokenLogic.finish(tokenOptions.getId(), tokenOptions.getUserResourceId(), start, new Date(), totalWritten);
            return true;
        }

        if (totalWritten < ACCUMULATED_NEEDED_TO_MARK_AS_STARTED) {
            // Ignore los primeros x bytes
            return true;
        }

        if (!started) {
            started = true;
            lastCheck = System.currentTimeMillis();
            System.out.println("[start] "+tokenOptions.getId()+" ("+ totalWritten +"/"+totalSize+")");
            // Si devuelve false se para la conexion, significa que no existe file_download y no ha podido ser actualizado, lo cual es muy improbable
            return tokenLogic.start(tokenOptions.getId(), start, totalSize);
        } else {
            // Update downloading
            long now = System.currentTimeMillis();
            double elapsedSinceLastCheck = now - lastCheck;

            double percent = ((double) totalWritten) / totalSize * 100;
            double percentDiffSinceLastCheck = percent - lastPercent;
            if (elapsedSinceLastCheck > UPDATE_EVERY_MILLIS_MAX || (percentDiffSinceLastCheck > 1 && elapsedSinceLastCheck > UPDATE_EVERY_MILLIS_MIN)) {
                lastPercent = percent;
                lastCheck = now;
                System.out.println("[downloading] reason("+elapsedSinceLastCheck+"millis "+percentDiffSinceLastCheck+"% diff) - download id:" + tokenOptions.getId() + " = " + percent + "% (" + totalWritten + "/" + totalSize + ")");
                // Si devuelve false se para la conexion, significa que no existe file_download y no ha podido ser actualizado, lo cual es muy improbable
                return tokenLogic.downloading(totalWritten, new Date(), tokenOptions.getId());
            }
        }
        return true;
    }

    void abort() {
        System.out.println("[abort] "+tokenOptions.getId()+" ("+ totalWritten +"/"+totalSize+")");
        tokenLogic.abort(totalWritten, start, new Date(), tokenOptions.getId(), tokenOptions.getUserResourceId());
    }

}
