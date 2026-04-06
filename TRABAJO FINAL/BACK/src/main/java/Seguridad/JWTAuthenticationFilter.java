package Seguridad;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.io.IOException;

// filtro que intercepta todas las requests para controlar acceso con JWT
@WebFilter(filterName = "jwt-auth-filter", urlPatterns = "/*")
public class JWTAuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

// 🔹 path SIN el context path (/TrabajoFinalCarballo)
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String method = req.getMethod();

        System.out.println("JWT filter => path = " + path + " | method = " + method);

        // ==== 1) Siempre dejar pasar OPTIONS (CORS) ====
        if (HttpMethod.OPTIONS.matches(method)) {
            chain.doFilter(request, response);
            return;
        }

        // ==== 2) Endpoints públicos específicos ====
        // - /jwt/auth  → login
        // - POST /usuarios → registrar usuario
        if ("/jwt/auth".equals(path)
                || ("/usuarios".equals(path) && HttpMethod.POST.matches(method))) {
            chain.doFilter(request, response);
            return;
        }

        // ===== GET públicos =====
        boolean esGetPublico =
                path.equals("/") ||
                        path.startsWith("/mascotas") && !req.getParameterMap().containsKey("usuarioId") ||
                        path.startsWith("/avistamientos") ||
                        path.startsWith("/publicaciones") ||
                        path.startsWith("/usuarios/ranking") ||
                        path.startsWith("/publicaciones/listarPublicaciones");
                ;

        // Si es GET y es público, se permite sin token
        if (HttpMethod.GET.matches(method) && esGetPublico) {
            chain.doFilter(request, response);
            return;
        }

        // ==== Endpoints protegidos: requieren Authorization: Bearer xxx ====
        // obtiene el token desde un header
        String token = req.getHeader(HttpHeaders.AUTHORIZATION);

        // si no tiene token o no es válido, responde 403 Forbidden
        if (token == null || !TokenServices.validateToken(token)) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        chain.doFilter(request, response);
    }
}
