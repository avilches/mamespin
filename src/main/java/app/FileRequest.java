/*
* @author Alberto Vilches
* @date 23/12/2016
*/
package app;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;

public class FileRequest {

	FileDownload fileDownload;

	public FileRequest() {
		fileDownload = new FileDownload();
	}

	
	@GET
	// Solo si el resolver validó el path, se llamará a este controlador, por lo queno es necesario validar nada
	public void service(ServletRequest servletRequest, ServletResponse servletResponse, @PathParam("path") String path) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		File file = new File("/Users/jerolba/Downloads/apertura greach.mov");
		fileDownload.serveResource(request, response, file);
	}

}
