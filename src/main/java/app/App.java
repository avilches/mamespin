/*
* @author Alberto Vilches
* @date 21/12/2016
*/
package app;

import app.download.MngServlet;
import app.download.TokenServlet;
import app.download.Downloader;
import app.download.TokenLogic;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
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

    public StatisticsHandler stats;
    public LowResourceMonitor lowResourcesMonitor;
    public long start;
    public int port;
    public String vhost;
    public Server server;
    public Downloader downloader;
    public Renderer renderer;
    public Resources resources;
    public Jedis jedis;
    public HikariDataSource ds;
    public TokenLogic tokenLogic;
    public Conf conf;

    public static void main(String[] args) throws Exception {
        String folder = args.length > 0 ? args[0] : "/srv/mamespin/config";
        new App().start(folder);
    }

    public App() {
    }

    private void start(String configFolder) throws Exception {
        start = System.currentTimeMillis();

        conf = new Conf();
        conf.load(configFolder+"/mamespin-common-config.groovy");
        conf.load(configFolder+"/filesrv-config.groovy");

        ds = new HikariDataSource();
        ds.setJdbcUrl(conf.getDataSourceProperty("url").toString());
        ds.setUsername(conf.getDataSourceProperty("username").toString());
        ds.setPassword(conf.getDataSourceProperty("password").toString());
        ds.setConnectionTimeout((int)conf.getDataSourceProperty("hikari.connectionTimeout"));
        ds.setConnectionInitSql(conf.getDataSourceProperty("hikari.connectionInitSql").toString());
        ds.setMaximumPoolSize((int)conf.getDataSourceProperty("hikari.maximumPoolSize"));
        ds.setMinimumIdle((int)conf.getDataSourceProperty("hikari.minimumIdle"));
        ds.setMaxLifetime((int)conf.getDataSourceProperty("hikari.maxLifetime"));

        port = conf.getServerPort();
        server = new Server(port);

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
        createMngServlet(ctx, "/mng/*");
        createStaticResourcesServlet(ctx, "/static/*", "/static");


        server.setStopAtShutdown(true);
        server.addLifeCycleListener(this);


        stats = new StatisticsHandler();
        stats.setHandler(ctx);
        server.setHandler(stats);


        lowResourcesMonitor=new LowResourceMonitor(server);
        lowResourcesMonitor.setPeriod(1000);
        lowResourcesMonitor.setLowResourcesIdleTimeout(200);
        lowResourcesMonitor.setMonitorThreads(true);
        lowResourcesMonitor.setMaxConnections(0);
        lowResourcesMonitor.setMaxMemory(0);
        lowResourcesMonitor.setMaxLowResourcesTime(5000);
        server.addBean(lowResourcesMonitor);

        server.start();

        System.out.println("Scheduling cleaner. Initial delay: "+conf.getCleanerDelay()+"min. Every: "+conf.getCleanerPeriod()+"min");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            AtomicBoolean busy = new AtomicBoolean(false);
            @Override
            public void run() {
                try {
                    if (!server.isRunning() || busy.get()) return;
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

    private void createMngServlet(ServletContextHandler rootContext, String path) {
        MngServlet servlet = new MngServlet();
        servlet.app = this;

        ServletHolder holder = new ServletHolder(servlet);
        holder.setInitOrder(0);
        rootContext.addServlet(holder, path);
    }

    private void createDownloadServlet(ServletContextHandler rootContext, String path) {
        TokenServlet servlet = new TokenServlet();
        servlet.server = server;
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
        rootContext.addServlet(holder, publicPath);
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
        System.out.println("Stopping...");
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        long s = (System.currentTimeMillis() - start)/1000;
        System.out.println("Stopped! Live " + String.format("%dh %02dm %02ds", (int)(s / 3600), (int)((s % 3600) / 60), (int)(s % 60)));
    }


}