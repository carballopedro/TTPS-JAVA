package com.example.clasificados;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@WebFilter("/*")
public class FiltroLogDeAccesos implements jakarta.servlet.Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        // tomo datos del request
        HttpServletRequest request=(HttpServletRequest) req;

        // ip del cliente
        String ip = request.getRemoteAddr();

        // fecha y hora
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        String fecha = "[" + ZonedDateTime.now().format(formatter) + "]";

        // request line
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        if (qs != null) uri += "?" + qs;
        String requestLine = "\"" + request.getMethod() + " " + uri + " " + request.getProtocol() + "\"";

        // user-agent
        String ua = request.getHeader("User-Agent");
        if (ua == null) ua = "-";

        // dejar pasar la cadena
        chain.doFilter(req, res);

        // log final
        System.out.println(ip + " - - " + fecha + " " + requestLine + " \"" + ua + "\"");    }
}

