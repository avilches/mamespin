/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app.download;

import app.Renderer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class DownloadServlet implements Servlet {
    public Downloader downloader;
    public Renderer renderer;
    public TokenLogic tokenLogic;

    public void init(ServletConfig config) throws ServletException {
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
                String token = request.getParameter("token");
                TokenLogic.Result resp = tokenLogic.checkToken(token, request.getRemoteAddr());
                if (resp.forbidden) {
                    forbidden(response);
                } else {
                    downloader.serve(request, response, file, resp.cpsPauser);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (!response.isCommitted()) {
                serverError(response);
            }
        } finally {
        }
    }

    private void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        renderer.render(response.getWriter(), "404");
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        renderer.render(response.getWriter(), "403");
    }

    private void unavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        renderer.render(response.getWriter(), "503");
    }

    private void serverError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        renderer.render(response.getWriter(), "500");
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }
}
