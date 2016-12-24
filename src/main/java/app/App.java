/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;

import java.io.File;

/**
 * Simple Jetty FileServer.
 * This is a simple example of Jetty configured as a FileServer.
 */

public class App implements LifeCycle.Listener {
    int port;
    Server server;
    long start = System.currentTimeMillis();

    public App(Server server, int port) {
        this.server = server;
        this.port = port;
    }

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

    public static void main(String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Server server = new Server(port);
        App app = new App(server, port);
        server.addLifeCycleListener(app);

        ServletContextHandler rootContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        rootContext.setContextPath("/");
        rootContext.setVirtualHosts(new String[] { "static.mamespin.com" });
        rootContext.setBaseResource(Resource.newClassPathResource("/"));
        createDownloadServlet(rootContext, "/download/*");
        createStaticResourcesServlet(rootContext, "/webapp", "/static/*");

        server.setHandler(rootContext);
        server.start();
        server.join();
    }

    private static void createStaticResourcesServlet(ServletContextHandler rootContext, String local, String publicPath) {
        ServletHolder holder = new ServletHolder(new DefaultServlet());
        holder.setInitOrder(0);
        String resourceStaticFolder = Resource.newClassPathResource(local).getName();
        System.out.println("Resource static folder: "+resourceStaticFolder);
        holder.setInitParameter("resourceBase", resourceStaticFolder);
        holder.setInitParameter("dirAllowed","true");
        holder.setInitParameter("pathInfoOnly","true");
        rootContext.addServlet(holder,publicPath);
    }

    private static void createDownloadServlet(ServletContextHandler rootContext, String path) {
        ServletHolder holder = new ServletHolder(new FileRequest());
        holder.setInitOrder(0);
        rootContext.addServlet(holder, path);
    }


}