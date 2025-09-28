package com.example.visitas50;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
        name = "Premio",
        value = "/Premio",
        initParams = {
                @WebInitParam(
                        name = "mensaje",
                        value = "¡Felicitaciones @! eres el visitante número # de nuestro sitio y has sido seleccionado para el gran premio TTPS - Cursada APROBADA"
                )
        }
)
    public class Premio extends HttpServlet {

        private int visitas = 0;
        private String plantilla; // cacheamos la plantilla
        private String ultimoUsuario = ""; // último usuario que accedió


    @Override
    public void init() throws ServletException {
        plantilla = getServletConfig().getInitParameter("mensaje");
        if (plantilla == null || plantilla.isBlank()) {
            plantilla = "Hola @, visita #";
        }
    }
    @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            visitas++;
            String nombre = request.getParameter("nombre");
        ultimoUsuario = nombre;

        // Reemplazos pedidos en el enunciado
        String mensaje = plantilla
                .replace("@", nombre)
                .replace("#", String.valueOf(visitas));

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Premio</title></head><body>");
            out.println("<h1>" + mensaje + "</h1>");
            out.println("</body></html>");
        }
        }

    // GET: si ?formato=json, devolver JSON con ultimoUsuario y visitas
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String formato = request.getParameter("formato");
        if ("json".equalsIgnoreCase(formato)) {
            response.setContentType("application/json;charset=UTF-8"); // Content-Type para JSON
            response.setHeader("Cache-Control", "no-store");
            String json = "{"
                    + "\"ultimoUsuario\":\"" + ultimoUsuario + "\","
                    + "\"visitas\":" + visitas
                    + "}";
            try (PrintWriter out = response.getWriter()) {
                out.print(json);
            }
        } else {
            // opcional: ayuda si entran sin formato=json
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("Usa ?formato=json para obtener JSON.");
        }
    }
    }
