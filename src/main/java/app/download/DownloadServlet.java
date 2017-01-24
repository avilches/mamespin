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

    public int[] cpss;

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
            String token = request.getParameter("token");
            final DbLogic.TokenOptions resp = tokenLogic.checkToken(token, request.getRemoteAddr());
            if (resp == null) {
                notFound(request, response, "Token incorrecto [subcode:1]");
            } else {

                String method = request.getMethod().toLowerCase();
                if (method.equals("get")) {
                    if (resp.isDownloading()) {
                        notFound(request, response, "Token incorrecto [subcode:2]");
//                        forbidden(request, response, "Te estás bajando este fichero ahora mismo");

                    } else if (resp.isFinished()) {
                        notFound(request, response, "Token incorrecto [subcode:3]");
//                        forbidden(request, response, "Ya te has bajado este fichero. Si necesitas descargarlo otra vez debes generar un nuevo enlace.");

                    } else if (resp.isSlotOverflow()) {
                        forbidden(request, response, "No tienes slots libres para iniciar esta descarga.");

                    }
                }
                File file = new File(resp.getPath());
                if (!file.exists() || file.length() == 0) {
                    notFound(request, response, "Local file not found in path");
                } else {
//                    info(request, response.getStatus(), "...");
                    CallbackDownload callback = new CallbackDownload(tokenLogic, resp, file.length());
                    int cps = cpss[Math.max(0, Math.min(resp.getLevel(), cpss.length - 1))];
                    downloader.serve(request, response, file, cps, false /* TODO: rangos. Ilimitado: ignorar validaciones slots/bajado/bajandose. Limitado: validar para solo dejar resumir */, callback);
                    long elapsed = System.currentTimeMillis() - start;
                    long KBs = file.length() / elapsed;

                    info(request, response.getStatus(), callback.accumulated+"/"+file.length()+". Speed: "+KBs+" KB/s");
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
