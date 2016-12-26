package app;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;

public class MameSpingErrorHandler extends ErrorHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		baseRequest.setHandled(true);
		String method = request.getMethod();
		if (!HttpMethod.GET.is(method) && !HttpMethod.POST.is(method) && !HttpMethod.HEAD.is(method)) {
			return;
		}

		response.setContentType(MimeTypes.Type.TEXT_HTML_8859_1.asString());
		try (ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(4096)) {
			writer.write("Error " + response.getStatus());
			writer.flush();
			response.setContentLength(writer.size());
			writer.writeTo(response.getOutputStream());
			writer.destroy();
		}
	}

}
