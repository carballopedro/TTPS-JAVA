package com.example.ttps2025_entregable1_carballo;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ListenerPochoclos implements ServletContextListener {

    public ListenerPochoclos() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // leo parámetro de inicialización de la aplicación
        Integer cantPochoclos = Integer.valueOf(sce.getServletContext().getInitParameter("cantPochoclos"));

        // se guarda en el contexto de la aplicación
        ServletContext contexto = sce.getServletContext();
        contexto.setAttribute("cantPochoclos", cantPochoclos);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is called when the servlet Context is undeployed or Application Server shuts down. */
    }
}