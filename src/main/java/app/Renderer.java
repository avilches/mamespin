/*
* @author Alberto Vilches
* @date 24/12/2016
*/
package app;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.PrintWriter;

public class Renderer {
    Configuration cfg;

    Renderer init(String basePath) {
        cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(this.getClass(), basePath);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        return this;
    }

    public void render(PrintWriter writer, String template) throws IOException {
        cfg.getTemplate(template+".ftl").dump(writer);
    }
}
