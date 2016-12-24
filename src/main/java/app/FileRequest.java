/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public class FileRequest implements Servlet {

    String page403;
    String page404;
    String page500;
    String page503;

    FileDownload fileDownload;

    Configuration cfg;

    void loadResources() throws IOException, URISyntaxException {
//        page403 = loadResource("403.html");
//        page404 = loadResource("404.html");
//        page500 = loadResource("500.html");
//        page503 = loadResource("503.html");
    }

    static String loadResource(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        String text = new Scanner(url.openStream()).useDelimiter("\\A").next();
        System.out.println(text);
        return text;
    }


    public void init(ServletConfig config) throws ServletException {
        try {
            loadResources();

            fileDownload = new FileDownload();

            ServletContext context = config.getServletContext();

            cfg = new Configuration(Configuration.VERSION_2_3_25);
            cfg.setServletContextForTemplateLoading(context, "/templates");
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {
            File file = new File("/users/avilches/Downloads/apertura greach.mov");
            if (!file.exists()) {
//                request.getRequestDispatcher("/404.ftl").forward(request, response);
                notFound(response);
            } else {
                fileDownload.serveResource(request, response, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            serverError(response);
        } catch (ServletException e) {
            e.printStackTrace();
            serverError(response);
        } finally {
        }
    }

    private void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        cfg.getTemplate("404.ftl").dump(response.getWriter());
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getOutputStream().print(page403);
    }

    private void unavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getOutputStream().print(page503);
    }

    private void serverError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getOutputStream().print(page500);
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {

    }
}
