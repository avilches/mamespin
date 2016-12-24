/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import app.download.CPSPauser;
import app.download.DownloadServlet;
import app.download.Downloader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;

public class App implements LifeCycle.Listener {

    long start;
    int port;
    Server server;
    Downloader downloader;
    Renderer renderer;
    Resources resources;

    public static void main(String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new App(port).start();
    }

    public App(int port) {
        this.port = port;
    }

    private void start() throws Exception {
        start = System.currentTimeMillis();

        downloader = new Downloader();
        renderer = new Renderer().init("/templates");
        resources = new Resources();

        ServletContextHandler ctx = createContext("static.mamespin.com", "/");
        createDownloadServlet(ctx, "/download/*");
        createStaticResourcesServlet(ctx, "/static/*", "/static");

        server = new Server(port);
        server.addLifeCycleListener(this);
        server.setHandler(ctx);
        server.start();
        server.join();
    }

    private ServletContextHandler createContext(String vhost, String path) {
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath(path);
        ctx.setVirtualHosts(new String[] { vhost });
        ctx.setBaseResource(Resource.newClassPathResource("/"));
        return ctx;
    }

    private void createDownloadServlet(ServletContextHandler rootContext, String path) {
        DownloadServlet servlet = new DownloadServlet();
        servlet.downloader = downloader;
        servlet.renderer = renderer;
        servlet.verySlow = CPSPauser.createInKBps(100);
        servlet.slow = CPSPauser.createInKBps(400);
        servlet.fast = CPSPauser.createInKBps(800);
        servlet.ultraFast = null;
        ServletHolder holder = new ServletHolder(servlet);
        holder.setInitOrder(0);
        rootContext.addServlet(holder, path);
    }

    private void createStaticResourcesServlet(ServletContextHandler rootContext, String publicPath, String local) {
        ServletHolder holder = new ServletHolder(new DefaultServlet());
        holder.setInitOrder(0);
        String resourceStaticFolder = Resource.newClassPathResource(local).getName();
        System.out.println("Resource static folder: "+resourceStaticFolder);
        holder.setInitParameter("resourceBase", resourceStaticFolder);
        holder.setInitParameter("dirAllowed","false");
        holder.setInitParameter("pathInfoOnly","true");
        holder.setInitParameter("gzip","true");
        rootContext.addServlet(holder,publicPath);
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


}