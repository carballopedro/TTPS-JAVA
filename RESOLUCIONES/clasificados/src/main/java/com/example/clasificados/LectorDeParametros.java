package com.example.clasificados;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class LectorDeParametros implements ServletContextListener {

    public LectorDeParametros() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        /* This method is called when the servlet context is initialized(when the Web application is deployed). */
        ServletContext ctx = sce.getServletContext();

        // Se leen parametros de inicializacion de la aplicación
        String nombre   = ctx.getInitParameter("nombreSitio");
        String email    = ctx.getInitParameter("emailContacto");
        String telefono = ctx.getInitParameter("telefonoContacto");

        // Se crea una instancia de SitioClasificado con los parametros leidos
        SitioClasificado sitio = new SitioClasificado(nombre, email, telefono);

        // Se guarda en el contexto
        ctx.setAttribute("sitio", sitio);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is called when the servlet Context is undeployed or Application Server shuts down. */
    }
}