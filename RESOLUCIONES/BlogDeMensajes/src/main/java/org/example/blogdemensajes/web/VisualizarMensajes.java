package org.example.blogdemensajes.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.example.blogdemensajes.dao.FactoryDAO;
import org.example.blogdemensajes.dao.MessageDAO;
import org.example.blogdemensajes.model.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/VisualizarMensajes")
public class VisualizarMensajes extends HttpServlet {


    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MessageDAO messageDAO = FactoryDAO.getMessageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String usernameParam = req.getParameter("username");
        List<Message> mensajes = (usernameParam != null && !usernameParam.isBlank())
                ? messageDAO.findByUser(usernameParam.trim())
                : messageDAO.findAll();

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang='es'><head>");
            out.println("<meta charset='UTF-8'/>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'/>");
            out.println("<title>Mensajes</title>");
            out.println("<link rel='stylesheet' href='css/estilos.css'>");
            out.println("</head><body>");

            out.println("<div class='card'>");
            out.println("<header><div class='title'>Mensajes</div></header>");
            out.println("<div class='list' style='padding:1rem;'>");

            if (mensajes.isEmpty()) {
                out.println("<p class='hint'>No hay mensajes para mostrar.</p>");
            } else {
                for (Message m : mensajes) {
                    String user  = m.getUser() != null ? m.getUser().getUsername() : "(sin usuario)";
                    String fecha = m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : "";

                    out.println("<div class='msg' style='margin-bottom:.6rem;border:1px solid #eee;border-radius:.5rem;padding:.6rem;background:#fff;'>");

                    // fila superior
                    out.println("<div style='display:flex; align-items:center; gap:.75rem;'>");

                    // izquierda: usuario + fecha (ocupa todo)
                    out.println("  <div style='flex:1'><strong>" + user + "</strong>"
                            + (fecha.isEmpty() ? "" : " · <span class='hint'>" + fecha + "</span>")
                            + "</div>");

                    // derecha: form del botón (anulamos estilos globales de form)
                    out.println("  <form method='post' action='EliminarMensaje' "
                            + "onsubmit=\"return confirm('¿Eliminar este mensaje?');\" "
                            + "style='margin:0 0 0 auto; padding:0; display:inline-block;'>");
                    out.println("    <input type='hidden' name='id' value='" + m.getId() + "'/>");
                    out.println("    <button type='submit'>Eliminar</button>");
                    out.println("  </form>");

                    out.println("</div>"); // fin fila

                    out.println("<div style='margin-top:.35rem;'>" + m.getTexto() + "</div>");
                    out.println("</div>");
                }
            }

            out.println("</div>"); // list
            out.println("<div style='padding:0 1rem 1rem;'>");
            out.println("<a class='link' href='agregarMensaje.html'>+ Agregar nuevo mensaje</a>");
            out.println("</div>");
            out.println("</div>"); // card

            out.println("</body></html>");
        }
    }
}