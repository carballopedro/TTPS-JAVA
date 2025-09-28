package com.example.clasificados;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

// comento @WebServlet para que me funcione el FiltroIdiomas
//@WebServlet("/LoginMultilenguaje")
public class LoginMultilenguaje extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Obtener nombre del archivo .properties desde el request (puesto por el filtro)
        String props = (String) req.getAttribute("propertiesName");
        if (props == null) props = "textos_es"; // default si no hay atributo

        // 2) Cargar el ResourceBundle
        ResourceBundle bundle = ResourceBundle.getBundle(props);

        // 3) Configurar la respuesta
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 4) Generar el HTML usando los textos del bundle
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"" + (props.endsWith("en") ? "en" : "es") + "\">");
        out.println("<head><meta charset='UTF-8'><title>" + bundle.getString("titulo") + "</title></head>");
        out.println("<body>");
        out.println("<h1>" + bundle.getString("titulo") + "</h1>");
        out.println("<form action='ServletLogin' method='post'>");
        out.println("<label>" + bundle.getString("labelusuario") + ":</label>");
        out.println("<input type='text' name='usuario' required><br>");
        out.println("<label>" + bundle.getString("labelpassword") + ":</label>");
        out.println("<input type='password' name='clave' required><br>");
        out.println("<input type='submit' value='" + bundle.getString("labellogin") + "'>");
        out.println("</form>");
        out.println("</body></html>");
    }
}