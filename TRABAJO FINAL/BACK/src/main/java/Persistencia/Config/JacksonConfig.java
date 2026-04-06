package Persistencia.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuración de Jackson.
// Permite serializar y deserializar correctamente tipos de fecha y hora de Java (LocalDate, LocalDateTime, etc.).

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // soporta LocalDate, LocalDateTime, etc.
        return mapper;
    }
}
