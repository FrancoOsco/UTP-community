package com.template.auth.service;

import com.template.auth.entity.RefreshToken;
import com.template.auth.entity.Usuario;
import com.template.auth.exception.RefreshTokenExpiradoException;
import com.template.auth.exception.RefreshTokenInvalidoException;
import com.template.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long DURACION_DIAS = 7;
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String crearRefreshToken(Usuario usuario) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        String tokenReal = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(generarHash(tokenReal));
        refreshToken.setFechaExpiracion(LocalDateTime.now().plusDays(DURACION_DIAS));
        refreshToken.setUsuario(usuario);

        refreshTokenRepository.save(refreshToken);
        return tokenReal;
    }

    @Transactional(noRollbackFor = RefreshTokenExpiradoException.class)
    public Usuario consumirRefreshToken(String tokenReal) {
        String tokenHash = generarHash(tokenReal);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenInvalidoException::new);

        if (refreshToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiradoException();
        }

        Usuario usuario = refreshToken.getUsuario();
        refreshTokenRepository.delete(refreshToken);
        return usuario;
    }

    @Transactional
    public void logout(String tokenReal) {
        String tokenHash = generarHash(tokenReal);

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    private String generarHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el hash del refresh token",
                    exception
            );
        }
    }
}