/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

/**
 * Simple Jetty FileServer.
 * This is a simple example of Jetty configured as a FileServer.
 */

public class App implements Servlet {

    Server server;
    ServletContextHandler context;
    static String page403;
    static String page404;
    static String page500;
    static String page503;

    FileDownload fileDownload;

    public void init(ServletConfig config) throws ServletException {
        fileDownload = new FileDownload();
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {

        try {
            File file = new File("/users/avilches/Downloads/apertura greach.mov");
            if (!file.exists()) {
                notFound(response);
            } else {
                fileDownload.serveResource((HttpServletRequest) request, (HttpServletResponse) response, file);
            }
        } catch (IOException e) {
            serverError(response);
        } catch (ServletException e) {
            serverError(response);
        } finally {
        }
    }

    private void notFound(ServletResponse response) throws IOException {
        response.getOutputStream().print(page404);
        ((HttpServletResponse)response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private void forbidden(ServletResponse response) throws IOException {
        response.getOutputStream().print(page403);
        ((HttpServletResponse)response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private void unavailable(ServletResponse response) throws IOException {
        response.getOutputStream().print(page503);
        ((HttpServletResponse)response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    private void serverError(ServletResponse response) throws IOException {
        response.getOutputStream().print(page500);
        ((HttpServletResponse)response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {

    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Server server = new Server(port);
        server.addLifeCycleListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStarting(LifeCycle event) {
                System.out.println("Starting...");
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                System.out.println("Started. Ready to rock in the " + port + " port! (" + (System.currentTimeMillis() - start) + "ms)");
            }

            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                System.out.println("Failure :" + (System.currentTimeMillis() - start));
            }

            @Override
            public void lifeCycleStopping(LifeCycle event) {
                System.out.println("Stopping :" + (System.currentTimeMillis() - start));
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                System.out.println("Stopped :" + (System.currentTimeMillis() - start));
            }
        });
        ServletContextHandler rootContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        App handler = new App();
        handler.context = rootContext;
        handler.server = server;
        rootContext.setContextPath("/sign");
        rootContext.setVirtualHosts(new String[] { "mamespin.com" });
        rootContext.setBaseResource(Resource.newResource("/"));
        server.setHandler(rootContext);

        rootContext.addServlet(App.class, "/hi");

        page404 = loadResource("404.html");
        page500 = loadResource("500.html");


        server.start();
        server.join();
    }

    private static String loadResource(String resource) throws IOException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        String text = new Scanner(url.openStream()).useDelimiter("\\A").next();
        return text;
    }


}