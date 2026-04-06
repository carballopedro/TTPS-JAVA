package SpringMVC;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// Configuración principal de Spring MVC.
// Define qué paquetes se escanean, habilita MVC, registra convertidores JSON
// y habilita el soporte para subida de archivos (multipart).

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "Controllers",
        "Modelo",
        "Persistencia",
        "Services",
        "Seguridad"
})
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }

    // Necesario para que Spring pueda resolver MultipartFile y RequestPart
    @Bean
    public org.springframework.web.multipart.MultipartResolver multipartResolver() {
        return new org.springframework.web.multipart.support.StandardServletMultipartResolver();
    }
}
