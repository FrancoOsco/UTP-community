package com.template.auth.security;

import com.template.auth.entity.Usuario;
import com.template.auth.repository.UsuarioRepository;
import com.template.auth.validation.CorreoInstitucional;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.esTokenValido(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.obtenerEmail(token);
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null || !usuario.isEmailVerificado() || !CorreoInstitucional.valido(usuario.getEmail())) {
            filterChain.doFilter(request, response);
            return;
        }

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(authority)
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
