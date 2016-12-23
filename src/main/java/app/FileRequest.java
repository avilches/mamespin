/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public class FileRequest implements Servlet {

    static String page403;
    static String page404;
    static String page500;
    static String page503;

    FileDownload fileDownload;

    static void loadResources() throws IOException, URISyntaxException {
        page403 = loadResources("403.html");
        page404 = loadResources("404.html");
        page500 = loadResources("500.html");
        page503 = loadResources("503.html");
    }

    private static String loadResources(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        String text = new Scanner(url.openStream()).useDelimiter("\\A").next();
        return text;
    }


    public void init(ServletConfig config) throws ServletException {
        fileDownload = new FileDownload();
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
                notFound(response);
            } else {
                fileDownload.serveResource(request, response, file);
            }
        } catch (IOException e) {
            serverError(response);
        } catch (ServletException e) {
            serverError(response);
        } finally {
        }
    }

    private void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getOutputStream().print(page404);
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
