package Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador raíz usado para verificar que la API esté levantada y funcionando.

@RestController
public class RootController {

    // para que me muestre que la API está funcionando
    @GetMapping("/")
    public String home() {
        return "API TrabajoFinalCarballo OK";
    }
}