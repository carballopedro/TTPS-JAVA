package com.example.clasificados;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

@WebFilter("/*") // Aplica este filtro a TODAS las rutas del sitio
public class FiltroIdiomas implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // --- Convertimos a HttpServletRequest/Response para usar métodos específicos ---
        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // --- 1) Resolver idioma del usuario ---
        // Paso 1: ¿vino por parámetro en la URL? (ej: ?lang=en)
        HttpSession session = request.getSession();
        String lang = request.getParameter("lang");

        // Paso 2: si no vino en la URL, busco lo que estaba guardado en la sesión
        if (lang == null || lang.isBlank()) lang = (String) session.getAttribute("lang");

        // Paso 3: si tampoco había nada en sesión, uso español por defecto
        if (lang == null) lang = "es";

        // Normalizo el valor: si empieza con "en" lo tomo como inglés, si no, español
        lang = lang.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "es";

        // Guardo la preferencia en sesión para que persista entre páginas
        session.setAttribute("lang", lang);

        // --- 2) Verificar si la petición es para un archivo HTML ---
        String path = request.getServletPath();           // ej: "/login.html"
        if ("/".equals(path)) path = "/index.html";       // si piden la raíz, sirvo index.html

        // Si NO es .html ni .htm, no lo toco: dejo pasar la request normal
        if (!(path.endsWith(".html") || path.endsWith(".htm"))) {
            chain.doFilter(req, res);
            return;
        }

        // --- 3) Cargar el ResourceBundle con los textos del idioma elegido ---
        // Busca automáticamente textos_es.properties o textos_en.properties
        ResourceBundle rb = ResourceBundle.getBundle("textos", new Locale(lang));

        // --- 4) Leer el contenido del archivo HTML desde el WAR ---
        try (InputStream in = request.getServletContext().getResourceAsStream(path)) {
            if (in == null) {
                // Si el archivo no existe, sigo con la cadena normal (Tomcat puede dar 404)
                chain.doFilter(req, res);
                return;
            }

            // Leo el HTML completo como String en UTF-8
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // --- 5) Reemplazar los placeholders {{clave}} por su valor ---
            for (String k : rb.keySet()) {
                html = html.replace("{{" + k + "}}", rb.getString(k));
            }

            // --- 6) Enviar la respuesta modificada al cliente ---
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");

            try (PrintWriter out = response.getWriter()) {
                out.write(html); // escribo el HTML ya traducido
            }

            // IMPORTANTE: hago return para cortar el flujo
            // (no llamo a chain.doFilter porque ya respondí yo)
            return;
        }
    }
}