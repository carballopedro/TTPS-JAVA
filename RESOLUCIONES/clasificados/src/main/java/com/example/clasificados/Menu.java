package com.example.clasificados;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/Menu")
public class Menu extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Tomamos el perfil que puso el servlet Login dentro del request
        String perfil = (String) req.getAttribute("perfil");

        if (perfil == null) {
            // Si por algún motivo no vino el perfil, vamos al error
            resp.getWriter().println("""
                   <html lang="es">
                   <head>
                       <meta charset="UTF-8">
                       <title>Pagina de Error</title>
                       <link rel="stylesheet" href="css/estilos.css">
    
                   </head>
                   <body>
                   <h1>Datos inválidos</h1>
                   <p>Los datos ingresados no son válidos. Por favor, intente nuevamente.</p>
                   <a href="login.html">Volver a la página de inicio de sesión</a>
                   </body>
                   </html>
                   """);
            return;
        }

        // Según el perfil, hacemos SendRedirect a la página correspondiente
        if ("Administrador".equalsIgnoreCase(perfil)) {

            // Incluir el encabezado dinámico antes del contenido del menú
            req.getRequestDispatcher("/encabezado-servlet").include(req, resp);

            resp.getWriter().println("""
                    <html lang="es">
                    <head>
                        <meta charset="UTF-8">
                        <title>Página de Administrador</title>
                        <link rel="stylesheet" href="css/estilos.css">
                    
                    </head>
                    <body>
                    <h1>Panel de Administración</h1>
                    
                    <input type="button" value="Listar Usuarios Publicadores">
                    <input type="button" value="ABM Administradores">
                    <input type="button" value="Ver Estadísticas">
                    </body>
                    </html>
                    """);
        } else if ("Publicador".equalsIgnoreCase(perfil)) {


            // Incluir el encabezado dinámico antes del contenido del menú
            req.getRequestDispatcher("/encabezado-servlet").include(req, resp);

            resp.getWriter().println("""
                    <html lang="es">
                    <head>
                        <meta charset="UTF-8">
                        <title>Página de Publicador</title>
                        <link rel="stylesheet" href="css/estilos.css">
                    
                    </head>
                    <body>
                    <h1>Panel de Publicador</h1>
                    
                    
                    <input type="button" value="Actualizar Datos de Contacto">
                    <input type="button" value="ABM de Publicaciones">
                    <input type="button" value="Contestar Consultas">
                    
                    </body>
                    </html>
                    """);
        } else {
            // Perfil desconocido → error
            resp.sendRedirect("error.html");
        }
    }
}