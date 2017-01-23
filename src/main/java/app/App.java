/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import app.download.CPSPauser;
import app.download.DownloadServlet;
import app.download.Downloader;
import app.download.TokenLogic;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import redis.clients.jedis.Jedis;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class App implements LifeCycle.Listener {

    long start;
    int port;
    String vhost;
    Server server;
    Downloader downloader;
    Renderer renderer;
    Resources resources;
    Jedis jedis;
    HikariDataSource ds;
    TokenLogic tokenLogic;

    public static void main(String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 7070 /* TODO: configurable*/;
        new App(port).start();
    }

    public App(int port) {
        this.port = port;
    }

    private void start() throws Exception {
        start = System.currentTimeMillis();

        ds = new HikariDataSource();
        /* TODO: configurable*/
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/mamespin?useUnicode=true&characterEncoding=UTF-8");
        ds.setUsername("root");
        ds.setPassword("");

        downloader = new Downloader();
        renderer = new Renderer().init("/templates");
        resources = new Resources();
        tokenLogic = new TokenLogic();
        tokenLogic.dbLogic = new DbLogic(ds, 2 /* TODO: configurable*/);
        tokenLogic.jedis = jedis;
        tokenLogic.ds = ds;

        // jedis = new Jedis("localhost");


        tokenLogic.dbLogic.cleanTokens(0);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            AtomicBoolean busy = new AtomicBoolean(false);
            @Override
            public void run() {
                try {
                    if (busy.get()) return;
                    busy.set(true);

                    // TODO: si hay varios servidores sirviendo, se debe guardar el id del servidor en la tabla file_download y cada servidor
                    // solo debe borrar sus propias peticiones perdidas. Tal y como esta ahora se borrarian las de todos
                    tokenLogic.dbLogic.cleanTokens();
                } catch(Exception e) {
                    e.printStackTrace(System.err);
                } finally {
                    busy.set(false);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1 /*TODO: configurable */),
            TimeUnit.MINUTES.toMillis(1 /*TODO: configurable */));


        ServletContextHandler ctx = createContext("mamespin-download.com" /* TODO: configurable*/, "/");
        createDownloadServlet(ctx, "/download/*");
        createStaticResourcesServlet(ctx, "/static/*", "/static");


        server = new Server(port);
        server.addLifeCycleListener(this);
        server.setHandler(ctx);
        server.start();
        server.join();
    }

    private ServletContextHandler createContext(String vhost, String path) {
        this.vhost = vhost;
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
        servlet.tokenLogic = tokenLogic;
        CPSPauser[] levels = new CPSPauser[7];
        levels[0] = CPSPauser.createInKBs(100);
        levels[1] = CPSPauser.createInKBs(200);
        levels[2] = CPSPauser.createInKBs(400);
        levels[3] = CPSPauser.createInKBs(800);
        levels[4] = CPSPauser.createInKBs(1000);
        levels[5] = CPSPauser.createInKBs(2000);
        levels[6] = null;
        servlet.levels = levels;

        ServletHolder holder = new ServletHolder(servlet);
        holder.setInitOrder(0);
        rootContext.addServlet(holder, path);
    }

    private void createStaticResourcesServlet(ServletContextHandler rootContext, String publicPath, String local) {
        ServletHolder holder = new ServletHolder(new DefaultServlet());
        holder.setInitOrder(0);
        String resourceStaticFolder = Resource.newClassPathResource(local).getName();
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
        System.out.println("Ready to rock! Use: http://" + vhost+":"+port + " (Started in " + (System.currentTimeMillis() - start) + "ms)");
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        System.out.println("Failure :" + (System.currentTimeMillis() - start));
        cause.printStackTrace(System.err);
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