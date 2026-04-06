package Controllers;

import Modelo.Enums.EstadoMascota;
import Modelo.Mascota;
import Modelo.Usuario;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

// Controlador REST que maneja las operaciones relacionadas con las mascotas.
// Permite crear, listar, editar y eliminar mascotas, así como consultar
// mascotas perdidas y las mascotas asociadas a un usuario.

@RestController
@RequestMapping("/mascotas")
public class MascotaController {

    private final MascotaDAO mascotaDAO;
    private final UsuarioDAO usuarioDAO;

    @Autowired
    public MascotaController(MascotaDAO mascotaDAO, UsuarioDAO usuarioDAO) {
        this.mascotaDAO = mascotaDAO;
        this.usuarioDAO = usuarioDAO;
    }

    // Listar todas las mascotas perdidas
    @GetMapping("/perdidas")
    public ResponseEntity<?> listarMascotasPerdidas() {

        List<Mascota> todas = mascotaDAO.getAll("id");

        List<Mascota> perdidas = todas.stream()
                .filter(m -> m.getEstado() == EstadoMascota.PERDIDO_PROPIO
                        || m.getEstado() == EstadoMascota.PERDIDO_AJENO)
                .toList();

        if (perdidas.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("No hay mascotas perdidas registradas.");
        }

        return new ResponseEntity<>(perdidas, HttpStatus.OK);
    }

    // Crear una nueva mascota
    @PostMapping
    public ResponseEntity<?> crearMascota(@RequestBody Mascota mascota) {

        if (mascota == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar los datos de la mascota a registrar.");
        }

        mascotaDAO.persist(mascota); // guardar

        return new ResponseEntity<>(mascota, HttpStatus.CREATED);
    }

    // Editar una mascota existente
    @PutMapping("/{id}")
    public ResponseEntity<?> editarMascota(@PathVariable("id") Long id,
                                                 @RequestBody Mascota datos) {

        if (datos == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No se enviaron datos para actualizar la mascota.");
        }

        Mascota actual = mascotaDAO.get(id);

        if (actual == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Mascota con id " + id + " no encontrada.");
        }

        // solo actualiza si se envía un valor !== null
        if (datos.getNombre() != null) actual.setNombre(datos.getNombre());
        if (datos.getColor() != null) actual.setColor(datos.getColor());
        if (datos.getTipo() != null) actual.setTipo(datos.getTipo());
        if (datos.getTamanio() != null) actual.setTamanio(datos.getTamanio());
        if (datos.getEstado() != null) actual.setEstado(datos.getEstado());
        if (datos.getRaza() != null) actual.setRaza(datos.getRaza());
        if (datos.getDuenio() != null) actual.setDuenio(datos.getDuenio());

        mascotaDAO.update(actual);

        return new ResponseEntity<>(actual, HttpStatus.OK);
    }

    // Eliminar una mascota
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMascota(@PathVariable("id") Long id) {

        // Buscar la mascota primero
        Mascota mascota = mascotaDAO.get(id);

        // si no existe la mascota
        if (mascota == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Mascota con id " + id + " no encontrada.");
        }

        //  verificar si tiene publicación asociada
        if (mascota.getPublicacion() != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar la mascota con id " + id +
                            " porque tiene una publicación asociada.");
        }

        // Si existe → eliminarla
        mascotaDAO.delete(mascota);

        // Eliminación correcta → 204 No Content
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("La mascota fue eliminada correctamente.");
    }

    // Listar todas las mascotas de un usuario
    // Recibe el id del usuario como parámetro de consulta (?usuarioId=)
    @GetMapping
    public ResponseEntity<?> listarMascotasDeUsuario(
            @RequestParam("usuarioId") Long usuarioId) {

        // 1. Verificar que el usuario exista
        Usuario u = usuarioDAO.get(usuarioId);
        if (u == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No existe un usuario con id " + usuarioId + ".");
        }

        // 2. Conseguir todas las mascotas
        List<Mascota> todas = mascotaDAO.getAll("id");

        // 3. Filtrar las del usuario
        List<Mascota> delUsuario = todas.stream()
                .filter(m -> m.getDuenio() != null
                        && m.getDuenio().getId() != null
                        && m.getDuenio().getId().equals(usuarioId))
                .toList();

        // 4. Si el usuario existe pero no tiene mascotas → 204
        if (delUsuario.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("El usuario existe pero no tiene mascotas registradas.");
        }

        // 5. Respuesta OK con sus mascotas
        return new ResponseEntity<>(delUsuario, HttpStatus.OK);
    }

}
