/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;

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
        rootContext.setContextPath("/sign");
        rootContext.setVirtualHosts(new String[] { "mamespin.com" });
        rootContext.setBaseResource(Resource.newResource("/"));
        server.setHandler(rootContext);

        FileRequest.loadResources();
        rootContext.addServlet(FileRequest.class, "/hi");

        server.start();
        server.join();
    }


}