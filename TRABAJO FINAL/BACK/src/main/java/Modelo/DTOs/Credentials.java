package Modelo.DTOs;

// DTO que representa las credenciales devueltas al autenticarse.
// Contiene el token JWT, su tiempo de expiración y el email del usuario.

public class Credentials {

    private String token;
    private int expiresIn;    // tiempo de validez en segundos
    private String username;  // email del usuario

    public Credentials() {
    }

    public Credentials(String token, int expiresIn, String username) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
