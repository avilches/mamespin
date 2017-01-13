/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app.download;

import app.DbLogic;
import app.Renderer;
import freemarker.template.TemplateException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DownloadServlet implements Servlet {
    public Downloader downloader;
    public Renderer renderer;
    public TokenLogic tokenLogic;

    public CPSPauser slow;
    public CPSPauser fast;

    public void init(ServletConfig config) throws ServletException {
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setAttribute("start", start);
        try {
/*
            File file = new File("/users/avilches/Downloads/apertura greach.mov");
            if (!file.exists()) {
                notFound(response);
            } else {
                downloader.serve(request, response, file, CPSPauser.createInKBps(600));
            }
*/

            String token = request.getParameter("token");
            final DbLogic.TokenOptions resp = tokenLogic.checkToken(token, request.getRemoteAddr());
            if (resp == null) {
                notFound(request, response, "Token invalido");

            } else if (resp.getState().equals("download")) {
                forbidden(request, response, "Te estás bajando este fichero ahora mismo");

            } else if (resp.getState().equals("finished")) {
                forbidden(request, response, "Ya te has bajado este fichero :-(");

            } else if (!resp.isUnlimited() && resp.getCurrentDownloads() >= 1 /* TODO: configurable por usuario, mostrar mensaje merjo*/) {
                forbidden(request, response, "Demasiadas descargas a la vez. Prueba otra vez cuando acabes las que tienes en curso.");

            } else {
                File file = new File(resp.getPath());
                if (!file.exists() || file.length() == 0) {
                    notFound(request, response, "Local file not found in path");
                } else {
                    info(request, response.getStatus(), "0 bytes...");
                    CallbackDownload callback = new CallbackDownload(tokenLogic, resp, file.length());
                    downloader.serve(request, response, file, resp.isUnlimited() ? fast : slow, false /* TODO algun dia hacerlo */, callback);
                    info(request, response.getStatus(), file.length()+" bytes.");
                }
            }

        } catch (TemplateException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            info(request, 500, e.getMessage());
            e.printStackTrace(System.err);
            if (!response.isCommitted()) {
                try {
                    serverError(request, response, "Unknown error");
                } catch (TemplateException e1) {
                    e1.printStackTrace(System.err);
                }
            }
        } finally {
        }
    }

    private void info(HttpServletRequest request, int status, String message) {
        long start = (long)request.getAttribute("start");
        long millis = System.currentTimeMillis() - start;

        String time = String.format("%02d:%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
            millis % TimeUnit.SECONDS.toMillis(1));

        String path = request.getServletPath() + (request.getQueryString() != null || !request.getQueryString().trim().equals("")? "?" + request.getQueryString() : "");
        System.out.println(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new Date()) + " " +
                request.getRemoteAddr() + " " +
                "["+request.getMethod() + "] "+status+" " +
                "["+time+"] "+
                path+" "+(message != null?message:""));
    }

    private void notFound(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        info(request, response.getStatus(), message);
        renderer.render(response.getWriter(), "404", message);
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
        info(request, response.getStatus(), message);
        renderer.render(response.getWriter(), "403", message);
    }

    private void unavailable(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, message);
        info(request, response.getStatus(), message);
        renderer.render(response.getWriter(), "503", message);
    }

    private void serverError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
        info(request, response.getStatus(), message);
        renderer.render(response.getWriter(), "500", message);
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }
}
