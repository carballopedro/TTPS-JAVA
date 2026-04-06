package Controllers;

import Modelo.DTOs.Credentials;
import Modelo.DTOs.UsernameAndPassword;
import Modelo.Usuario;
import Persistencia.DAO.Interfaces.UsuarioDAO;
import Seguridad.TokenServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST encargado de la autenticación de usuarios.
// Recibe email y contraseña, valida las credenciales y, si son correctas,
// genera y devuelve un token JWT para el acceso al sistema.

@RestController
public class LoginController {

    @Autowired
    private UsuarioDAO usuarioDAO;

    @Autowired
    private TokenServices tokenServices;

    // tiempo de validez del token (en segundos)
    private final int EXPIRATION_IN_SEC = 1800; // p.ej. 30 min

    // Recibo user y pass del cliente
    @PostMapping(path = "/jwt/auth")
    public ResponseEntity<?> authenticate(@RequestBody UsernameAndPassword userpass) {

        // validación mínima del body
        if (userpass == null ||
                userpass.getUsername() == null ||
                userpass.getPassword() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Debe enviar email y contraseña para iniciar sesión.");
        }

        // Si las credenciales son correctas, se genera el token
        if (isLoginSuccess(userpass.getUsername(), userpass.getPassword())) {
            String token = tokenServices.generateToken(userpass.getUsername(), EXPIRATION_IN_SEC);
            Credentials creds = new Credentials(token, EXPIRATION_IN_SEC, userpass.getUsername());
            return ResponseEntity.ok(creds);
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Usuario o password incorrecto");
        }
    }

    // helper privado
    private boolean isLoginSuccess(String email, String rawPassword) {
        Usuario existente = usuarioDAO.getByEmail(email);
        if (existente == null) {
            return false;
        }
        // compara password enviada con el hash guardado en BD
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(rawPassword, existente.getPassword());
    }
}
