package com.example.clasificados;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet("/ServletLogin")   // URL que se usa en el action del formulario (login.html)
public class Login extends HttpServlet {

    // Clase interna para representar un usuario
    private static class Usuario {
        private final String nombre;
        private final String clave;
        private final String perfil; // Puede ser "Administrador" o "Publicador"

        Usuario(String nombre, String clave, String perfil) {
            this.nombre = nombre;
            this.clave = clave;
            this.perfil = perfil;
        }
    }

    // Lista donde vamos a guardar los usuarios válidos
    private List<Usuario> usuarios;

    // Se ejecuta al iniciar el servlet (una sola vez)
    @Override
    public void init() throws ServletException {
        usuarios = new ArrayList<>();

        // Cargamos usuarios de ejemplo (pueden ser más)
        usuarios.add(new Usuario("admin", "1234", "Administrador"));
        usuarios.add(new Usuario("pepe", "abcd", "Publicador"));
    }

    // Maneja el POST (cuando se envía el formulario)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Obtenemos los datos enviados desde el formulario
        String usuario = req.getParameter("usuario");
        String clave   = req.getParameter("clave");

        // Buscamos si hay un usuario que coincida con nombre y clave
        Optional<Usuario> match = usuarios.stream()
                .filter(u -> u.nombre.equals(usuario) && u.clave.equals(clave))
                .findFirst();

        // Si encontramos un usuario válido
        if (match.isPresent()) {
            String perfil = match.get().perfil;

            // Guardamos el perfil como atributo dentro del request
            req.setAttribute("perfil", perfil);

            // Delegamos la petición al servlet Menú
            req.getRequestDispatcher("Menu").forward(req, resp);

        } else {
            // Si no se encontró, redirigimos a la página de error (puede ser forward también)
            resp.sendRedirect("error.html");
        }
    }

    // Si entran con GET (por URL), los mando al login directamente
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.sendRedirect("login.html");
    }
}