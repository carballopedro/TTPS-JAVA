package org.example.blogdemensajes.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.example.blogdemensajes.dao.FactoryDAO;
import org.example.blogdemensajes.dao.MessageDAO;

import java.io.IOException;

@WebServlet("/EliminarMensaje")
public class EliminarMensaje extends HttpServlet {

    private final MessageDAO messageDAO = FactoryDAO.getMessageDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        String username = req.getParameter("username"); // para volver al filtro si lo había

        if (idParam != null && !idParam.isBlank()) {
            try {
                long id = Long.parseLong(idParam);
                messageDAO.delete(id);
            } catch (NumberFormatException ignored) { /* id inválido: no hacemos nada */ }
        }

        // redirige al listado (mantiene filtro si vino)
        String base = req.getContextPath() + "/VisualizarMensajes";
        if (username != null && !username.isBlank()) {
            resp.sendRedirect(base + "?username=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8));
        } else {
            resp.sendRedirect(base);
        }
    }
}