package com.template.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method) || !esEndpointProtegido(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");

        if (origin == null || !origin.equals(frontendUrl)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                      "status": 403,
                      "message": "Origen de solicitud no permitido."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean esEndpointProtegido(String path) {
        return path.equals("/api/auth/refresh")
                || path.equals("/api/auth/logout");
    }
}
