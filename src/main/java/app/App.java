/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

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
    Conf conf;

    public static void main(String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : -1;
        new App(port).start();
    }

    public App(int port) {
        this.port = port;
    }

    private void start() throws Exception {
        start = System.currentTimeMillis();

        conf = new Conf();
        conf.load("/Users/avilches/.grails/mamespin-common-config.groovy"); // TODO: parametro
        conf.load("/Users/avilches/.grails/filesrv-config.groovy"); // TODO: parametro

        ds = new HikariDataSource();
        ds.setJdbcUrl(conf.getDataSourceProperty("url").toString());
        ds.setUsername(conf.getDataSourceProperty("username").toString());
        ds.setPassword(conf.getDataSourceProperty("password").toString());
        ds.setConnectionTimeout((int)conf.getDataSourceProperty("hikari.connectionTimeout"));
        ds.setConnectionInitSql(conf.getDataSourceProperty("hikari.connectionInitSql").toString());
        ds.setMaximumPoolSize((int)conf.getDataSourceProperty("hikari.maximumPoolSize"));
        ds.setMinimumIdle((int)conf.getDataSourceProperty("hikari.minimumIdle"));
        ds.setMaxLifetime((int)conf.getDataSourceProperty("hikari.maxLifetime"));

        port = port > 0 ? port : conf.getServerPort();

        downloader = new Downloader();
        renderer = new Renderer().init("/templates");
        resources = new Resources();
        tokenLogic = new TokenLogic();
        tokenLogic.dbLogic = new DbLogic(ds);
        tokenLogic.jedis = jedis;
        tokenLogic.ds = ds;

        // jedis = new Jedis("localhost");

        // TODO: si hay varios servidores sirviendo, se debe guardar el id del servidor en la tabla file_download y cada servidor
        // solo debe borrar sus propias peticiones perdidas. Tal y como esta ahora se borrarian las de todos
        tokenLogic.dbLogic.cleanTokens(0);

        ServletContextHandler ctx = createContext(conf.getVirtualHost(), "/");
        createDownloadServlet(ctx, "/download/*");
        createStaticResourcesServlet(ctx, "/static/*", "/static");


        server = new Server(port);
        server.addLifeCycleListener(this);
        server.setHandler(ctx);
        server.start();

        System.out.println("Scheduling cleaner. Initial delay: "+conf.getCleanerDelay()+"min. Every: "+conf.getCleanerPeriod()+"min");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            AtomicBoolean busy = new AtomicBoolean(false);
            @Override
            public void run() {
                try {
                    if (busy.get()) return;
                    busy.set(true);

                    // TODO: si hay varios servidores sirviendo, se debe guardar el id del servidor en la tabla file_download y cada servidor
                    // solo debe borrar sus propias peticiones perdidas. Tal y como esta ahora se borrarian las de todos
                    tokenLogic.dbLogic.cleanTokens(conf.getCleanerTimeout());
                } catch(Exception e) {
                    e.printStackTrace(System.err);
                } finally {
                    busy.set(false);
                }
            }
        }, TimeUnit.MINUTES.toMillis(conf.getCleanerDelay()),
           TimeUnit.MINUTES.toMillis(conf.getCleanerPeriod()));

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
        servlet.cpss = conf.loadCps();
        servlet.cpsMsgs = conf.loadCpsMsgs();

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