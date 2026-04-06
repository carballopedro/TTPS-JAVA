package Controllers;


import Modelo.DTOs.UsuarioPerfilDTO;
import Modelo.DTOs.UsuarioRankingDTO;
import Modelo.Usuario;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Importo BCrypt para trabajar con contraseñas sin usar texto plano
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import org.springframework.http.HttpHeaders;
import Seguridad.TokenServices;

import java.util.List;

// Controlador REST que maneja las operaciones relacionadas con los usuarios.
// Permite registrar y editar usuarios, obtener el perfil del usuario autenticado
// mediante JWT y consultar el ranking de usuarios según sus puntos.

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    // para hashear contraseña
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UsuarioDAO usuarioDAO;

    @Autowired
    public UsuarioController (UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // Registrar un nuevo usuario
    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {

        if (usuario == null ||
                usuario.getNombre() == null ||
                usuario.getApellido() == null ||
                usuario.getEmail() == null ||
                usuario.getPassword() == null ||
                usuario.getTelefono() == null ||
                usuario.getBarrio() == null ||
                usuario.getCiudad() == null ||
                usuario.getLatitud() == null ||
                usuario.getLongitud() == null) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Faltan datos obligatorios para registrar el usuario.");
        }

        // email único
        Usuario existente = usuarioDAO.getByEmail(usuario.getEmail());
        if (existente != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Ya existe un usuario con email " + usuario.getEmail());
        }

        // Se hashea la contraseña antes de guardarla en la base de datos
        String hash = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(hash);

        usuarioDAO.persist(usuario);

        return new ResponseEntity<>(usuario, HttpStatus.CREATED);
    }

    // Login de usuario
    // endpoint que quedó como alternativa de login sin JWT; el principal es /jwt/auth en LoginController
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario datosLogin) {

        if (datosLogin == null || datosLogin.getEmail() == null || datosLogin.getPassword() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar correo y contraseña para iniciar sesión.");
        }

        Usuario existente = usuarioDAO.getByEmail(datosLogin.getEmail());

        if (existente == null) {
            // usuario no encontrado
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No se encontró un usuario con el correo " + datosLogin.getEmail() + ".");
        }


        // Valida la contraseña usando el hash almacenado
        boolean ok = passwordEncoder.matches(datosLogin.getPassword(), existente.getPassword());
        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("La contraseña ingresada es incorrecta.");
        }

        // login ok
        return new ResponseEntity<>(existente, HttpStatus.OK);
    }

    // Editar usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> editarUsuario(@PathVariable("id") Long id,
                                                 @RequestBody Usuario datos) {

        if (datos == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No se enviaron datos para actualizar el usuario.");
        }

        Usuario actual = usuarioDAO.get(id);

        if (actual == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Usuario con id " + id + " no encontrado.");
        }

        // solo actualiza si se envía un valor !== null
        if (datos.getNombre() != null) {
            actual.setNombre(datos.getNombre());
        }

        if (datos.getApellido() != null) {
            actual.setApellido(datos.getApellido());
        }

        if (datos.getEmail() != null) {
            actual.setEmail(datos.getEmail());
        }

        if (datos.getTelefono() != null) {
            actual.setTelefono(datos.getTelefono());
        }

        if (datos.getBarrio() != null) {
            actual.setBarrio(datos.getBarrio());
        }

        if (datos.getCiudad() != null) {
            actual.setCiudad(datos.getCiudad());
        }

        if (datos.getLatitud() != null) {
            actual.setLatitud(datos.getLatitud());
        }

        if (datos.getLongitud() != null) {
            actual.setLongitud(datos.getLongitud());
        }

        if (datos.getInstagram() != null) {
            actual.setInstagram(datos.getInstagram());
        }

        if (datos.getSitioWeb() != null) {
            actual.setSitioWeb(datos.getSitioWeb());
        }

        // Si viene una contraseña nueva, se vuelve a hashear
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            String hash = passwordEncoder.encode(datos.getPassword());
            actual.setPassword(hash);
        }

        usuarioDAO.update(actual);

        return new ResponseEntity<>(actual, HttpStatus.OK);
    }

    // Obtener el perfil del usuario autenticado usando el JWT
    @GetMapping("/perfil")
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerPerfil(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("No se recibió el token de autorización.");
        }

        // email que guardamos como subject en el token
        String email = TokenServices.getUsernameFromToken(authHeader);

        if (email == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Token inválido o expirado.");
        }

        Usuario usuario = usuarioDAO.getByEmail(email);

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No se encontró un usuario con el correo " + email + ".");
        }

        UsuarioPerfilDTO perfil = new UsuarioPerfilDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getBarrio(),
                usuario.getCiudad(),
                usuario.getLatitud(),
                usuario.getLongitud(),
                usuario.getPuntos(),
                usuario.isHabilitado(),
                usuario.getInstagram(),
                usuario.getSitioWeb(),
                usuario.getDescripcion(),
                usuario.getBadges() == null
                        ? List.of()
                        : new java.util.ArrayList<>(usuario.getBadges())
        );


        // Devuelve todos los datos del usuario (sin password y sin relaciones ignoradas)
        return new ResponseEntity<>(perfil, HttpStatus.OK);
    }

    // Obtener ranking de usuarios por puntos
    @Transactional(readOnly = true)
    @GetMapping("/ranking")
    public ResponseEntity<?> rankingUsuarios(
            @RequestParam(value = "limit", required = false) Integer limit) {

        List<Usuario> todos = usuarioDAO.getAll("puntos");

        // Filtrar habilitados y ordenar de mayor a menor
        List<Usuario> ordenados = todos.stream()
                .filter(Usuario::isHabilitado)
                .sorted((u1, u2) -> u2.getPuntos().compareTo(u1.getPuntos()))
                .toList();

        // Aplicar limit si lo pasan desde el front
        if (limit != null && limit > 0 && limit < ordenados.size()) {
            ordenados = ordenados.subList(0, limit);
        }

        // Mapear a DTO
        List<UsuarioRankingDTO> ranking = ordenados.stream()
                .map(u -> new UsuarioRankingDTO(
                        u.getId(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getBarrio(),
                        u.getCiudad(),
                        u.getPuntos(),
                        u.getBadges() == null ? List.of() : new java.util.ArrayList<>(u.getBadges())   // << badge del usuario
                        // fuerza la carga dentro de la sesión y devuelve una lista común
                ))
                .toList();

        if (ranking.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("No hay usuarios para el ranking.");
        }

        return new ResponseEntity<>(ranking, HttpStatus.OK);
    }

}
