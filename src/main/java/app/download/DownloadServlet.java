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

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {

            validateThenExecute(request, response);

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

    private void validateThenExecute(HttpServletRequest request, HttpServletResponse response) throws IOException, TemplateException, ServletException {
        String token = request.getParameter("token");

        DbLogic.TokenOptions tokenOptions = tokenLogic.checkToken(token, request.getRemoteAddr());
        if (tokenOptions == null) {
            notFound(request, response, "Token incorrecto [subcode:1]");
            return;
        }

        String method = request.getMethod().toLowerCase();

        if (!method.equals("get") && !method.equals("head")) {
            notAllowed(request, response, "Metodo no permitido");
            return;
        }

        boolean isGet = method.equals("get");
        if (isGet) {
            // Solo se validan los get
            if (tokenOptions.isDownloading()) {
                notFound(request, response, "Token incorrecto [subcode:2]");
//                        forbidden(request, response, "Te estás bajando este fichero ahora mismo");
                return;

            } else if (tokenOptions.isFinished()) {
                notFound(request, response, "Token incorrecto [subcode:3]");
//                        forbidden(request, response, "Ya te has bajado este fichero. Si necesitas descargarlo otra vez debes generar un nuevo enlace.");
                return;

            } else if (tokenOptions.isSlotOverflow()) {
                forbidden(request, response, "No tienes slots libres para iniciar esta descarga.");
                return;
            }
        }

        File file = new File(tokenOptions.getPath());
        if (!file.exists() || file.length() == 0) {
            notFound(request, response, "Local file not found in path");
            return;
        }

        executeDownload(request, response, tokenOptions, file);
    }

    private void executeDownload(HttpServletRequest request, HttpServletResponse response, DbLogic.TokenOptions tokenOptions, File file) throws IOException, ServletException {
        long start = System.currentTimeMillis();
        request.setAttribute("start", start);
//        info(request, response.getStatus(), "...");
        DownloadHandler callback = new DownloadHandler(tokenLogic, tokenOptions, file.length());
        int cps = getCPSFromLevel(tokenOptions.getLevel());
        downloader.serve(request, response, file, cps, false /* TODO: rangos. Ilimitado: ignorar validaciones slots/bajado/bajandose. Limitado: validar para solo dejar resumir */, callback);
        long elapsed = System.currentTimeMillis() - start;
        long KBs = callback.totalWritten / elapsed;
        info(request, response.getStatus(), callback.totalWritten + "/" + file.length() + ". Speed: " + KBs + " KB/s");
    }

    private int getCPSFromLevel(int level) {
        int securePos = Math.max(0, Math.min(level, cpss.length - 1));
        return cpss[securePos];
    }

    private void info(HttpServletRequest request, int status, String message) {
        long start = (long) request.getAttribute("start");
        long millis = System.currentTimeMillis() - start;

        String time = String.format("%02d:%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
                millis % TimeUnit.SECONDS.toMillis(1));

        String path = request.getServletPath() + (request.getQueryString() != null || !request.getQueryString().trim().equals("") ? "?" + request.getQueryString() : "");
        System.out.println(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new Date()) + " " +
                request.getRemoteAddr() + " " +
                "[" + request.getMethod() + "] " + status + " " +
                "[" + time + "] " +
                path + " " + (message != null ? message : ""));
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

    private void notAllowed(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
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
