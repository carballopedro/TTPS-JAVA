package Controllers;

import Modelo.Avistamiento;
import Services.AvistamientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador REST que maneja los endpoints de avistamientos.
// Permite listar avistamientos registrados y crear nuevos,
// delegando la lógica de negocio al AvistamientoService.

@RestController
@RequestMapping("/avistamientos")
public class AvistamientoController {

    private final AvistamientoService avistamientoService;

    @Autowired
    public AvistamientoController(AvistamientoService avistamientoService) {
        this.avistamientoService = avistamientoService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {

        List<Avistamiento> avistamientos = avistamientoService.listarTodos();

        if (avistamientos == null || avistamientos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("No hay avistamientos registrados.");
        }

        return ResponseEntity.ok(avistamientos);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Avistamiento av) {

        // Validación básica
        if (av == null ||
                av.getPublicacion() == null ||
                av.getPublicacion().getId() == null) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Faltan datos obligatorios para registrar el avistamiento (publicacion.id).");        }

        // Llamo al service
        Avistamiento creado = avistamientoService.crear(av);

        // Si el service devolvió null, es porque la publicación NO existe → 404
        if (creado == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No se encontró la publicación con id " + av.getPublicacion().getId() + ".");
        }

        // Creación exitosa → 201
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }
}