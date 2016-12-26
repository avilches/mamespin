package app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.otogami.web.controller.ClassControllerHolder;
import com.otogami.web.controller.ControllerHolder;
import com.otogami.web.resolver.JaxRsPackageScanResolver;
import com.otogami.web.resolver.ResolverChain;
import com.otogami.web.resolver.RouteResolver;
import com.otogami.web.resolver.SimpleMapResolver;
import com.otogami.web.results.Ok;

public class JeroApp {

	public static void main(String[] args) throws Exception {
		JettyServer server = new JettyServer();
		server.withPort(8080);
		server.withRoutes(()->new Mapping());
		server.run();
	}
	
	public static class Mapping extends ResolverChain {
		public Mapping() {
			//Añade un resolver con lógica sobre qué urls son válidas o no. Es una inner class para ver el ejemplo
			this.addResolver(new DownloadRouteResolver());
			//Escanea todas las clases del paquete buscando controladores
			this.addResolver(new JaxRsPackageScanResolver(HelloWorld.class));
			//Se crea un controlador con una lambda
			this.addResolver(new SimpleMapResolver("/hola", "GET", (ServletRequest req, ServletResponse res)-> {
				Map<String,Object> model = new HashMap<>();
				model.put("name", "torpedo");
				return new Ok("HelloWorld.ftl", model);
			}));
		}
	}
	
	public static class DownloadRouteResolver implements RouteResolver {

		private static final String DOWNLOAD="/download/";
		
		@Override
		public ControllerHolder resolve(String path, String method) {
			if (path.startsWith(DOWNLOAD)) {
				String tail=path.substring(DOWNLOAD.length());
				if (someComplexLogicToLocateFile(tail)) {
					Map<String, Object> map = new HashMap<>();
					map.put("path", tail);
					return new ClassControllerHolder(FileRequest.class, map);
				}
			}
			return null;
		}
		
		@Override
		public List<String> explain() {
			return Arrays.asList("GET	/download/*");
		}

		private boolean someComplexLogicToLocateFile(String file) {
			if (file.equals("foo")) {
				return true;
			}
			return false;
		}
		
	}
}
