/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app.download;

import app.App;
import freemarker.template.TemplateException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.eclipse.jetty.server.LowResourceMonitor;
import tools.StringTools;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MngServlet implements Servlet {
    public App app;
    public void init(ServletConfig config) throws ServletException {
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        try {

            String action = request.getRequestURI().length() > 5 ? request.getRequestURI().substring(5) /* "/mng/" */: null;
            if (action != null) {
                if (!validateThenExecute(action, request, response)) {
                    response.sendError(404);
                }
            } else {
                response.sendError(404);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } finally {
        }
    }

    private void showStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(200);
        LowResourceMonitor lowResourcesMonitor = app.lowResourcesMonitor;
        response.getWriter().print("<h2>Low resources</h2>");
        response.getWriter().print("Low: "+ lowResourcesMonitor.isLowOnResources()+"<br/>");
        response.getWriter().print("Started: "+(lowResourcesMonitor.getLowResourcesStarted() > 0 ? (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(lowResourcesMonitor.getLowResourcesStarted()))):"")+"<br/>");
        response.getWriter().print("Reasons: "+ lowResourcesMonitor.getLowResourcesReasons()+"<br/>");
        response.getWriter().print(app.stats.toStatsHTML());

        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long memory = total - free;
        response.getWriter().print("<h2>Memory</h2>"+ StringTools.humanReadableString(memory) +" occupied + "+StringTools.humanReadableString(free)+" free = "+StringTools.humanReadableString(total)+" total <br/>");
        response.getWriter().print("<h2>JVM</h2>Uptime: "+ new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(app.start)) + "<br/><br/>");
        response.getWriter().print(DefaultGroovyMethods.join(ManagementFactory.getRuntimeMXBean().getInputArguments(), "<br/>"));
    }

    private boolean validateThenExecute(String action, HttpServletRequest request, HttpServletResponse response) throws IOException, TemplateException, ServletException {
        boolean processed = true;
        if (action.equals("shutdown")) {
            if (!request.getParameter("key").equals("6omb63htb4ojstywv0rwlcsbsp9jv5ofwkgqh08ep9z1dqh2")) {
                processed = false;
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            app.server.stop();
                            Thread.sleep(1000);
                            System.exit(0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
            }
        } else if (action.equals("status")) {
            showStatus(request, response);
        } else {
            processed = false;
        }
        return processed;
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }
}
