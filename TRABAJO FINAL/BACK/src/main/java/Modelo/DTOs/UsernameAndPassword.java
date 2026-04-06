package Modelo.DTOs;

// DTO usado para recibir las credenciales de login.
// Contiene el email (username) y la contraseña ingresados por el usuario.
public class UsernameAndPassword {

    // en username vas a mandar el email
    private String username;
    private String password;

    public UsernameAndPassword() {
    }

    public UsernameAndPassword(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
