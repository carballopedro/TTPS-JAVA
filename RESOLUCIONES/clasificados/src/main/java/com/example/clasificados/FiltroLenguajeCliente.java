package com.example.clasificados;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

// comento @WebServlet para que me funcione el FiltroIdiomas
//@WebFilter("/*") // Se ejecuta para todas las peticiones
public class FiltroLenguajeCliente implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        // 1) Obtener el Locale preferido del cliente según Accept-Language
        Locale clientLocale = request.getLocale();

        // 2) Determinar idioma: si empieza con "en" uso inglés, sino español
        String lang = (clientLocale != null && clientLocale.getLanguage().toLowerCase().startsWith("en"))
                ? "en"
                : "es";

        // 3) Armar el nombre del archivo .properties (sin extensión)
        String propertiesName = "textos_" + lang; // p. ej.: textos_es o textos_en

        // 4) Guardar este atributo en el request (lo pide la consigna)
        request.setAttribute("propertiesName", propertiesName);

        // 5) Continuar con el resto de filtros/servlets
        chain.doFilter(req, res);
    }
}