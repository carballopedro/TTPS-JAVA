package org.example.blogdemensajes.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.blogdemensajes.dao.FactoryDAO;
import org.example.blogdemensajes.dao.MessageDAO;
import org.example.blogdemensajes.dao.UserDAO;
import org.example.blogdemensajes.model.Message;
import org.example.blogdemensajes.model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/GuardarMensaje")
public class GuardarMensaje extends HttpServlet {

    private final MessageDAO messageDAO = FactoryDAO.getMessageDAO();
    private final UserDAO userDAO = FactoryDAO.getUserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String username = trim(req.getParameter("username"));
        String texto    = trim(req.getParameter("texto"));

        // Validaciones mínimas
        if (username.isEmpty() || texto.isEmpty() ||
                username.length() > 50 || texto.length() > 500) {
            redirectWithError(req, resp,
                    "Datos inválidos. Completá usuario y mensaje.",
                    username, texto);
            return;
        }

        // Verificar existencia del usuario
        User u = userDAO.findByUserName(username);
        if (u == null) {
            redirectWithError(req, resp,
                    "Usuario inexistente.",
                    username, texto);
            return;
        }

        // Guardar mensaje
        Message m = new Message(texto, u);
        messageDAO.create(m);

        // Éxito: ver listado de mensajes
        resp.sendRedirect(req.getContextPath() + "/VisualizarMensajes");
    }

    /* Utils */

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void redirectWithError(HttpServletRequest req, HttpServletResponse resp,
                                   String error, String username, String texto) throws IOException {
        String base = req.getContextPath() + "/agregarMensaje.html";
        String url = base + "?error=" + enc(error) + "&username=" + enc(username) + "&texto=" + enc(texto);
        resp.sendRedirect(url);
    }
}