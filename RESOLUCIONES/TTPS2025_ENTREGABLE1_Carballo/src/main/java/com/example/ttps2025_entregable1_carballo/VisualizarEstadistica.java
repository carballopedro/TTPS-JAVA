package com.example.ttps2025_entregable1_carballo;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet("/VisualizarEstadistica")
public class VisualizarEstadistica extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        ServletContext ctx = getServletContext();
        Map<String, Integer> ventas = (Map<String, Integer>) ctx.getAttribute("ventasPorPelicula");

        out.println("<!DOCTYPE html>");
        out.println("<html lang='es'><head>" +
                "<meta charset='UTF-8'>" +
                "<title>Estadísticas</title>" +
                "<link rel=\"stylesheet\" href=\"css/estilos.css\">" +
                "</head><body style='background-color:#fff3e0; margin:0; font-family:Arial, sans-serif;'>");

        out.println("<div style='text-align:center; margin-top: 3rem;'>");
        out.println("<h1 style='color:#ff7043; font-size:2rem; margin-bottom: 1.5rem;'>"
                + "Estadística de Entradas Vendidas</h1>");

        if (ventas == null || ventas.isEmpty()) {
            out.println("<p style='font-size:1.2rem;'>No hay ventas registradas todavía.</p>");
        } else {
            out.println("<ul style='list-style:none; padding:0; display:inline-block; text-align:left;'>");
            for (Map.Entry<String, Integer> entry : ventas.entrySet()) {
                out.println("<li style='margin-bottom:0.5rem; font-size:1.2rem;'>"
                        + "<b>" + entry.getKey() + ":</b> "
                        + entry.getValue() + " entradas</li>");
            }
            out.println("</ul>");
        }
        out.println("</div>");
        out.println("</body></html>");
    }
}