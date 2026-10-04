package com.template.auth.service;

import com.template.auth.dto.AuthTokens;
import com.template.auth.dto.LoginRequest;
import com.template.auth.dto.UsuarioRequest;
import com.template.auth.entity.Usuario;
import com.template.auth.exception.CredencialesInvalidasException;
import com.template.auth.exception.EmailNoVerificadoException;
import com.template.auth.exception.EmailYaRegistradoException;
import com.template.auth.repository.UsuarioRepository;
import com.template.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void register(UsuarioRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailYaRegistradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol("USUARIO");
        usuario.setEmailVerificado(false);

        usuarioRepository.save(usuario);
        emailVerificationService.crearVerificacion(usuario);
    }

    public AuthTokens login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByEmail(request.getEmail())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        if (!usuario.isEmailVerificado()) {
            throw new EmailNoVerificadoException();
        }

        String accessToken = jwtService.generarToken(usuario.getEmail());
        String refreshToken = refreshTokenService.crearRefreshToken(usuario);

        return new AuthTokens(accessToken, refreshToken);
    }

    public AuthTokens refresh(String refreshToken) {
        Usuario usuario = refreshTokenService.consumirRefreshToken(refreshToken);
        String nuevoAccessToken = jwtService.generarToken(usuario.getEmail());
        String nuevoRefreshToken = refreshTokenService.crearRefreshToken(usuario);

        return new AuthTokens(nuevoAccessToken, nuevoRefreshToken);
    }
}