/*
* @author Alberto Vilches
* @date 24/12/2016
*/
package app;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Resources {
    Map<String, String> cache = new HashMap<>();
    String loadResource(String resource) throws IOException, URISyntaxException {
        if (cache.containsKey(resource)) {
            return cache.get(resource);
        }
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        String text = new Scanner(url.openStream()).useDelimiter("\\A").next();
        System.out.println(text);
        cache.put(resource, text);
        return text;
    }

}
