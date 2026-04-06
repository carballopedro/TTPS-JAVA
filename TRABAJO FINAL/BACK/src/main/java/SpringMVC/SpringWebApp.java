package SpringMVC;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;


// Inicializador de la aplicación Spring MVC.
// Reemplaza el web.xml, crea el contexto de Spring,
// registra el DispatcherServlet y habilita soporte para multipart (subida de archivos).

public class SpringWebApp implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext container) throws ServletException {

        // Create the 'root' Spring application context
        AnnotationConfigWebApplicationContext rootContext =
                new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);

        // ContextLoaderListener - Manage the lifecycle of the root application context
        container.addListener(new ContextLoaderListener(rootContext));

        // DispatcherServlet - Register and map the dispatcher servlet
        ServletRegistration.Dynamic dispatcher = container.addServlet("DispatcherServlet",
                new DispatcherServlet(rootContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        // Habilitar soporte multipart (OBLIGATORIO para @RequestPart / MultipartFile)
        MultipartConfigElement multipartConfig = new MultipartConfigElement(
                null,          // location (usa el tmp del servidor)
                5_000_000L,    // maxFileSize: 5 MB
                20_000_000L,   // maxRequestSize: 20 MB
                0              // fileSizeThreshold
        );
        dispatcher.setMultipartConfig(multipartConfig);
    }
}