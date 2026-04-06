package Seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

// Servicio encargado de generar, validar y leer tokens JWT.
// Permite autenticar usuarios sin usar sesiones (stateless).

@Service
public class TokenServices {

    // Clave secreta para firmar/verificar el token (se genera al levantar la app)
    private static final SecretKey key =
            (SecretKey) Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Genera un token de autorización para el usuario.
     *
     * @param username username/email que se guarda en el token
     * @param segundos tiempo de validez en segundos
     * @return token JWT firmado
     */
    public String generateToken(String username, int segundos) {
        Date exp = getExpiration(new Date(), segundos);

        return Jwts.builder()
                .setSubject(username)      // payload: subject = username (en tu caso, email)
                .setExpiration(exp)        // fecha de expiración
                .signWith(key)             // firma con la clave HS256
                .compact();                // compacta a String
    }

    // Calcula la fecha de expiración del token
    private Date getExpiration(Date base, int segundos) {
        return new Date(base.getTime() + segundos * 1000L);
    }

    /**
     * Valida el token recibido en el header Authorization.
     * Devuelve true si el token es válido y no está expirado.
     */
    public static boolean validateToken(String token) {
        String prefix = "Bearer";

        try {
            if (token.startsWith(prefix)) {
                token = token.substring(prefix.length()).trim();
            }

            // parsea y verifica la firma usando la misma key
            // obtiene el payload
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Expiration: " + claims.getExpiration());

            return true;
        } catch (ExpiredJwtException exp) {
            // token expirado - acceso denegado
            System.out.println("Token expirado: " + exp.getMessage());
            return false;
        } catch (JwtException e) {
            // token corrupto - acceso inválido
            System.out.println("Token inválido: " + e.getMessage());
            return false;
        }
    }

    // Obtiene el username (email) del token JWT
    public static String getUsernameFromToken(String token) {
        String prefix = "Bearer";

        try {
            if (token.startsWith(prefix)) {
                token = token.substring(prefix.length()).trim();
            }

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();  // acá está el email

        } catch (ExpiredJwtException exp) {
            System.out.println("Token expirado al obtener username: " + exp.getMessage());
            return null;
        } catch (JwtException e) {
            System.out.println("Token inválido al obtener username: " + e.getMessage());
            return null;
        }
    }
}
