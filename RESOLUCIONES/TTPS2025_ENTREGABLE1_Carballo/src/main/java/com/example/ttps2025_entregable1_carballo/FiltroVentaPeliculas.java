package com.example.ttps2025_entregable1_carballo;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebFilter("/ImprimeEntrada")
public class FiltroVentaPeliculas implements Filter {

    private ServletContext ctx;
    private Map<String, Integer> ventas;

    @Override
    public void init(FilterConfig filterConfig) {
        ctx = filterConfig.getServletContext();
        ventas = new HashMap<>();
        ctx.setAttribute("ventasPorPelicula", ventas);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String pelicula = req.getParameter("pelicula");
        String cantidadStr = req.getParameter("cantidad");

        if (pelicula != null && cantidadStr != null) {
            try {
                int cantidad = Integer.parseInt(cantidadStr);
                ventas.put(pelicula, ventas.getOrDefault(pelicula, 0) + cantidad);
            } catch (NumberFormatException ignored) {
                // si por algún motivo "cantidad" no es un número, lo ignoro
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}