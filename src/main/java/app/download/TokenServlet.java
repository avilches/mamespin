/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app.download;

import app.DbLogic;
import app.Renderer;
import freemarker.template.TemplateException;
import org.eclipse.jetty.server.Server;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TokenServlet implements Servlet {
    public Downloader downloader;
    public Renderer renderer;
    public TokenLogic tokenLogic;
    public Server server;

    public int[] cpss;
    public String[] cpsMsgs;
    public ConcurrentHashMap liveTokens = new ConcurrentHashMap();

    public void init(ServletConfig config) throws ServletException {
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        servletRequest.setAttribute("start", start);

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {

            validateThenExecute(request, response);

        } catch (TemplateException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            info(request, 500, e.getMessage());
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
        DbLogic.TokenOptions tokenOptions = null;
        try {
            if (request.getRequestURI().length() < 50) {
                notFound(request, response, "Token incorrecto [subcode:0]");
                return;
            }
            String token = request.getRequestURI().substring(10); // "/download/"

            tokenOptions = tokenLogic.checkToken(token, request.getRemoteAddr());
            if (tokenOptions == null) {
                notFound(request, response, "Token incorrecto [subcode:1]");
                return;
            }
            tokenOptions.setCps(getCPSFromLevel(tokenOptions.getLevel()));
            tokenOptions.setCpsMsg(getCPSMsgFromLevel(tokenOptions.getLevel()));

            register(tokenOptions);

            String method = request.getMethod().toLowerCase();

            if (!method.equals("get") && !method.equals("head")) {
                notAllowed(request, response, "Metodo no permitido");
                return;
            }

            boolean isGet = method.equals("get");
            if (isGet) {

                if (request.getParameter("info") != null) {
                    showInfo(request, response, tokenOptions);
                    return;
                }

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


            if (!tokenOptions.getFile().exists() || tokenOptions.getFile().length() == 0) {
                notFound(request, response, "Local file not found in path");
                return;
            }

            executeDownload(request, response, tokenOptions);
        } finally {
            unregister(tokenOptions);
        }
    }

    private void unregister(DbLogic.TokenOptions tokenOptions) {
        if (tokenOptions == null) return;
        // TODO: si metemos rangos y se permite descargar a la vez el mismo token (con cada uno un rango) esto no va a funcionar
        // TODO: posible solucion solo meter como live el primer rango...

//        liveTokens.remove(tokenOptions.getId());
    }

    private void register(DbLogic.TokenOptions tokenOptions) {
//        liveTokens.put(tokenOptions.getId(), tokenOptions);
    }

    private void executeDownload(HttpServletRequest request, HttpServletResponse response, DbLogic.TokenOptions tokenOptions) throws IOException, ServletException {
//        info(request, response.getStatus(), "...");
        File file = tokenOptions.getFile();
        DownloadHandler callback = new DownloadHandler(server, tokenLogic, tokenOptions, file.length());
        downloader.serve(request, response, file, tokenOptions.getFilename(), tokenOptions.getCps(), false /* TODO: rangos. Ilimitado: ignorar validaciones slots/bajado/bajandose. Limitado: validar para solo dejar resumir */, callback);
        long start = (long) request.getAttribute("start");
        long elapsed = System.currentTimeMillis() - start;
        long KBs = callback.totalWritten / elapsed;
        info(request, response.getStatus(), callback.totalWritten + "/" + file.length() + ". Speed: " + KBs + " KB/s");
    }

    private int getCPSFromLevel(int level) {
        int securePos = Math.max(0, Math.min(level, cpss.length - 1));
        return cpss[securePos];
    }

    private String getCPSMsgFromLevel(int level) {
        int securePos = Math.max(0, Math.min(level, cpss.length - 1));
        return cpsMsgs[securePos];
    }

    private void info(HttpServletRequest request, int status, String message) {
        long start = (long) request.getAttribute("start");
        long millis = System.currentTimeMillis() - start;

        String time = String.format("%02d:%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
                millis % TimeUnit.SECONDS.toMillis(1));

        String path = request.getServletPath() + (request.getQueryString() != null && request.getQueryString().trim().length() > 0 ? "?" + request.getQueryString() : "");
        System.out.println(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new Date()) + " " +
                request.getRemoteAddr() + " " +
                "[" + request.getMethod() + "] " + status + " " +
                "[" + time + "] " +
                path + " " + (message != null ? message : ""));
    }

    private void showInfo(HttpServletRequest request, HttpServletResponse response, DbLogic.TokenOptions options) throws IOException, TemplateException {
        response.setStatus(HttpServletResponse.SC_OK);
        info(request, response.getStatus(), "info");
        renderer.render(new PrintWriter(response.getOutputStream()), "info", "opts", options);
    }

    private void notFound(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        info(request, response.getStatus(), message);
        renderer.render(new PrintWriter(response.getOutputStream()), "404", message);
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        info(request, response.getStatus(), message);
        renderer.render(new PrintWriter(response.getOutputStream()), "403", message);
    }

    private void notAllowed(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        info(request, response.getStatus(), message);
        renderer.render(new PrintWriter(response.getOutputStream()), "405", message);
    }

    private void serverError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException, TemplateException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        info(request, response.getStatus(), message);
        renderer.render(new PrintWriter(response.getOutputStream()), "500", message);
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }
}
