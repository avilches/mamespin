package app;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.otogami.web.results.Ok;
import com.otogami.web.results.Result;

public class HelloWorld {

	@GET
	@Path("hello")
	public Result someMethod(@QueryParam("name") String name) {
		Map<String,Object> model = new HashMap<>();
		
		//Some complex logic :D
		if (name==null) {
			name = "Betauer";
		}
		model.put("name", name.toUpperCase());
		return new Ok("HelloWorld.ftl", model);
	}
}
