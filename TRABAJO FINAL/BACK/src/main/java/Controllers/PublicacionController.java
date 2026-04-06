package Controllers;

import Modelo.DTOs.PublicacionPublicaDTO;
import Modelo.Enums.Badge;
import Modelo.Enums.EstadoMascota;
import Modelo.Mascota;
import Modelo.Publicacion;
import Modelo.PublicacionFoto;
import Modelo.Usuario;
import Persistencia.DAO.Interfaces.MascotaDAO;
import Persistencia.DAO.Interfaces.PublicacionDAO;
import Persistencia.DAO.Interfaces.PublicacionFotoDAO;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import Seguridad.TokenServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

// Controlador REST que maneja todas las operaciones relacionadas con las publicaciones.
// Permite crear, listar, editar y eliminar publicaciones, gestionar fotos,
// controlar permisos mediante JWT y actualizar estados de mascotas y puntos de usuarios.

@RestController
@RequestMapping("/publicaciones")
public class PublicacionController {

    private final PublicacionDAO publicacionDAO;
    private final MascotaDAO mascotaDAO;
    private final UsuarioDAO usuarioDAO;
    private final PublicacionFotoDAO publicacionFotoDAO;

    /**
     * Ajusta el badge del usuario según sus puntos:
     * 0-29   -> BRONCE
     * 30-59  -> PLATA
     * 60+   -> ORO
     */
    private void actualizarBadgePorPuntos(Usuario u) {
        int puntos = u.getPuntos() != null ? u.getPuntos() : 0;

        Badge nuevoBadge = (puntos >= 60) ? Badge.ORO
                : (puntos >= 30) ? Badge.PLATA
                : Badge.BRONCE;

        u.setBadges(new java.util.ArrayList<>(java.util.List.of(nuevoBadge)));
    }

    @Autowired
    public PublicacionController(PublicacionDAO publicacionDAO,
                                 MascotaDAO mascotaDAO,
                                 UsuarioDAO usuarioDAO,
                                 PublicacionFotoDAO publicacionFotoDAO) {
        this.publicacionDAO = publicacionDAO;
        this.mascotaDAO = mascotaDAO;
        this.usuarioDAO = usuarioDAO;
        this.publicacionFotoDAO = publicacionFotoDAO;
    }

    // Crear una nueva publicación
    @Transactional(rollbackFor = Exception.class)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearPublicacion(
            @RequestPart("publicacion") Publicacion pub,
            @RequestPart("fotos") List<MultipartFile> fotos,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader)
            throws IOException {

        // Body inválido / datos obligatorios
        if (pub == null ||
                pub.getMascota() == null ||
                pub.getCreador() == null || pub.getCreador().getId() == null) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Faltan datos obligatorios para crear la publicación (mascota y creador).");
        }

        // Verifico fotos
        if (fotos == null || fotos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("La publicación debe incluir al menos una foto.");
        }

        Usuario u = null;

        // 1) Intento obtener usuario desde el token
        if (authHeader != null && !authHeader.isBlank()) {
            String email = TokenServices.getUsernameFromToken(authHeader);
            if (email != null) {
                u = usuarioDAO.getByEmail(email);

                // Si el front mandó otro id distinto al del token => lo bloqueo
                if (u != null && pub.getCreador() != null && pub.getCreador().getId() != null
                        && !u.getId().equals(pub.getCreador().getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("No podés crear publicaciones en nombre de otro usuario.");
                }
            }
        }

        // 2) Si no hubo token (o falló), uso el id que manda el front (como antes)
        if (u == null) {
            u = usuarioDAO.get(pub.getCreador().getId());
        }

        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encontró un usuario creador con id " + pub.getCreador().getId() + ".");
        }

        // Sumar 10 puntos al creador (manejo de null)
        int puntosActuales = u.getPuntos() != null ? u.getPuntos() : 0;
        u.setPuntos(puntosActuales + 10);

        // actualizar badge según puntos
        actualizarBadgePorPuntos(u);

        usuarioDAO.update(u);

        // Mascota nueva (se crea junto con la publicación)
        Mascota nuevaMascota = pub.getMascota();
        mascotaDAO.persist(nuevaMascota);     // genera el id en BD

        // Asigno entidades “reales” de BD
        pub.setMascota(nuevaMascota);
        pub.setCreador(u);

        // Fecha actual si no se envió
        if (pub.getFecha() == null) {
            pub.setFecha(LocalDate.now());
        }

        // Agrego fotos
        for (MultipartFile file : fotos) {
            PublicacionFoto pf = new PublicacionFoto();
            pf.setFoto(file.getBytes());
            pub.addFoto(pf);
        }


        // activo la publicacion
        pub.setActiva(true);

        publicacionDAO.persist(pub);

        return new ResponseEntity<>(pub, HttpStatus.CREATED);
    }

    // Listar todas las publicaciones
    @Transactional(readOnly = true)
    @GetMapping("/listarPublicaciones")
    public ResponseEntity<?> listarPublicaciones() {

        List<Publicacion> todas = publicacionDAO.getAll("id");

        if (todas.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("No hay publicaciones registradas.");
        }


        // Publicacion DTO que agrega los datos del creador
        List<PublicacionPublicaDTO> dtos = todas.stream()
                .map(PublicacionPublicaDTO::new)
                .toList();

        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    // Listar publicaciones de un usuario (por id de creador)
    @Transactional(readOnly = true)
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> listarPublicacionesDeUsuario(
            @PathVariable("usuarioId") Long usuarioId) {

        // 1. Verificar que el usuario exista
        Usuario u = usuarioDAO.get(usuarioId);
        if (u == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No existe un usuario con id " + usuarioId + ".");
        }

        // 2. Traer todas las publicaciones
        List<Publicacion> todas = publicacionDAO.getAll("id");

        // 3. Filtrar por creador
        List<Publicacion> delUsuario = todas.stream()
                .filter(p -> p.getCreador() != null
                        && p.getCreador().getId() != null
                        && p.getCreador().getId().equals(usuarioId))
                .toList();

        if (delUsuario.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("El usuario existe pero no tiene publicaciones registradas.");
        }

        return new ResponseEntity<>(delUsuario, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarPublicacion(
            @PathVariable("id") Long id,
            @RequestBody Publicacion datos,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {

        // 1) VALIDAR TOKEN
        String email = TokenServices.getUsernameFromToken(authHeader);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Token inválido o expirado.");
        }

        Usuario usuarioLogueado = usuarioDAO.getByEmail(email);
        if (usuarioLogueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Usuario del token no encontrado.");
        }

        // 2) OBTENER PUBLICACIÓN REAL
        Publicacion actual = publicacionDAO.get(id);
        if (actual == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Publicación no encontrada.");
        }

        // 3) VERIFICAR QUE EL LOGUEADO SEA EL CREADOR
        if (actual.getCreador() == null ||
                actual.getCreador().getId() == null ||
                !actual.getCreador().getId().equals(usuarioLogueado.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el creador puede editar esta publicación.");
        }

        // 4) EDITAR SOLO DESCRIPCIÓN
        if (datos.getDescripcion() != null) {
            actual.setDescripcion(datos.getDescripcion());
        }

        // 5) EDITAR SOLO NOMBRE DE LA MASCOTA
        if (actual.getMascota() != null &&
                datos.getMascota() != null &&
                datos.getMascota().getNombre() != null) {

            actual.getMascota().setNombre(datos.getMascota().getNombre());
        }

        // 6) GUARDAR CAMBIOS
        publicacionDAO.update(actual);

        return ResponseEntity.ok(actual);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPublicacion(
            @PathVariable("id") Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        // 1. Obtener email/username desde el token
        String email = TokenServices.getUsernameFromToken(authHeader);
        if (email == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Token inválido o expirado.");
        }

        // 2. Buscar usuario logueado (ajustá el método al que tengas en tu DAO)
        Usuario usuarioLogueado = usuarioDAO.getByEmail(email);
        if (usuarioLogueado == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Usuario del token no encontrado.");
        }

        // 3. Buscar publicación
        Publicacion pub = publicacionDAO.get(id);
        if (pub == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Publicación con id " + id + " no encontrada.");
        }

        // 4. Verificar que la publicación sea del usuario logueado
        if (pub.getCreador() == null ||
                pub.getCreador().getId() == null ||
                !pub.getCreador().getId().equals(usuarioLogueado.getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Solo el creador de la publicación puede eliminarla.");
        }

        // 5. Borrar publicación (fotos se borran por cascade, y mascota si tenés cascade en la relación)
        publicacionDAO.delete(pub);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("La publicación fue eliminada correctamente.");
    }

    // Obtener publicaciones recientes (ordenadas por fecha desc, con limit opcional)
    @Transactional(readOnly = true)
    @GetMapping("/recientes")
    public ResponseEntity<?> publicacionesRecientes(
            @RequestParam(value = "limit", required = false) Integer limit) {

        // 1. Obtener todas las publicaciones ordenadas por fecha
        List<Publicacion> todas = publicacionDAO.getAll("fecha");

        if (todas.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body("No hay publicaciones registradas.");
        }

        // 2. Orden descendente (más recientes primero)
        List<Publicacion> ordenadas = todas.stream()
                .sorted((p1, p2) -> p2.getFecha().compareTo(p1.getFecha()))
                .toList();

        // 3. Aplicar limit si lo paso desde el front
        if (limit != null && limit > 0 && limit < ordenadas.size()) {
            ordenadas = ordenadas.subList(0, limit);
        }

        // 4. Mapear a DTO
        List<PublicacionPublicaDTO> dtos = ordenadas.stream()
                .map(PublicacionPublicaDTO::new)
                .toList();

        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }


    // obtener una foto por id
    @GetMapping("/fotos/{fotoId}")
    public ResponseEntity<byte[]> obtenerFoto(@PathVariable("fotoId") Long fotoId) {

        PublicacionFoto pf = publicacionFotoDAO.get(fotoId);
        if (pf == null || pf.getFoto() == null) {
            return ResponseEntity.notFound().build();
        }

        // Por ahora asumimos JPEG. Si querés soportar PNG, etc.,
        // podés guardar también el content-type en la entidad.
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")  // opcional
                .contentType(MediaType.IMAGE_JPEG)
                .body(pf.getFoto());
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoMascota(
            @PathVariable("id") Long id,
            @RequestParam("accion") String accion,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {

        // 1) VALIDAR TOKEN
        String email = TokenServices.getUsernameFromToken(authHeader);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Token inválido o expirado.");
        }

        Usuario usuarioLogueado = usuarioDAO.getByEmail(email);
        if (usuarioLogueado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Usuario del token no encontrado.");
        }

        // 2) OBTENER PUBLICACIÓN
        Publicacion pub = publicacionDAO.get(id);
        if (pub == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Publicación no encontrada.");
        }

        // 3) OBTENER MASCOTA
        Mascota m = pub.getMascota();
        if (m == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La publicación no tiene mascota asociada.");
        }

        EstadoMascota nuevoEstado;

        // ==== ADOPTAR ====
        if ("Adoptar".equalsIgnoreCase(accion)) {

            if (m.getEstado() != EstadoMascota.PERDIDO_AJENO) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Solo se puede adoptar una mascota en estado PERDIDO_AJENO.");
            }

            nuevoEstado = EstadoMascota.ADOPTADO;

            // sumar puntos al usuario que adopta (logueado)
            int puntosActuales = usuarioLogueado.getPuntos() != null
                    ? usuarioLogueado.getPuntos()
                    : 0;
            usuarioLogueado.setPuntos(puntosActuales + 10);

            // actualizar badge según puntos
            actualizarBadgePorPuntos(usuarioLogueado);

            usuarioDAO.update(usuarioLogueado);

            // ==== RECUPERAR ====
        } else if ("Recuperar".equalsIgnoreCase(accion)) {

            if (m.getEstado() == EstadoMascota.PERDIDO_PROPIO) {
                // en perdido_propio solo el creador puede marcar como recuperado
                if (pub.getCreador() == null ||
                        pub.getCreador().getId() == null ||
                        !pub.getCreador().getId().equals(usuarioLogueado.getId())) {

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Solo el creador puede marcar como RECUPERADO una mascota PERDIDO_PROPIO.");
                }
            } else if (m.getEstado() == EstadoMascota.PERDIDO_AJENO) {
                // perdido_ajeno: cualquier usuario logueado puede recuperarla
                // (no chequeamos creador)
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Solo se puede recuperar una mascota PERDIDO_PROPIO o PERDIDO_AJENO.");
            }

            nuevoEstado = EstadoMascota.RECUPERADO;

        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Acción inválida. Usá 'Adoptar' o 'Recuperar'.");
        }

        // 4) ACTUALIZAR ESTADO
        m.setEstado(nuevoEstado);
        m.setDuenio(usuarioLogueado);
        mascotaDAO.update(m);

        return ResponseEntity.ok("Estado actualizado a " + nuevoEstado);
    }
}
