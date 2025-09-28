package com.example.clasificados;

import java.io.*;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet (name = "encabezado", value = "/encabezado-servlet")
public class Encabezado extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SitioClasificado sitio = (SitioClasificado) getServletContext().getAttribute("sitio");

        PrintWriter out = resp.getWriter(); //

        if (sitio == null) {
            out.println("<header><strong>Sitio no configurado</strong></header>");
            return; // sin cerrar
        }

        out.println("<h1>" + sitio.getNombre() + "</h1>");
        out.println("<p>Email: " + sitio.getEmail() + " | Tel: " + sitio.getTelefono() + "</p>");
        out.println("</header>");
    }

    //opcional
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

}
