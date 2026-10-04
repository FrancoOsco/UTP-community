package com.template.auth.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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

        String key = request.getRemoteAddr() + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> crearBucket(path));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {
                  "status": 429,
                  "message": "Demasiadas solicitudes. Intenta nuevamente más tarde."
                }
                """);
    }

    private boolean esEndpointProtegido(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password")
                || path.equals("/api/auth/verify-email")
                || path.equals("/api/auth/refresh");
    }

    private Bucket crearBucket(String path) {
        return switch (path) {
            case "/api/auth/login" ->
                    crearBucket(5, Duration.ofMinutes(1));
            case "/api/auth/register",
                 "/api/auth/reset-password",
                 "/api/auth/verify-email" ->
                    crearBucket(5, Duration.ofMinutes(10));
            case "/api/auth/forgot-password" ->
                    crearBucket(3, Duration.ofMinutes(10));
            case "/api/auth/refresh" ->
                    crearBucket(10, Duration.ofMinutes(1));
            default ->
                    crearBucket(100, Duration.ofMinutes(1));
        };
    }

    private Bucket crearBucket(long capacidad, Duration duracion) {
        return Bucket.builder()
                .addLimit(limit ->
                        limit.capacity(capacidad)
                                .refillGreedy(capacidad, duracion)
                )
                .build();
    }
}