package com.template.auth.service;

import com.template.auth.entity.EmailVerificationToken;
import com.template.auth.entity.Usuario;
import com.template.auth.exception.VerificationTokenExpiradoException;
import com.template.auth.exception.VerificationTokenInvalidoException;
import com.template.auth.repository.EmailVerificationTokenRepository;
import com.template.auth.repository.UsuarioRepository;
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
public class EmailVerificationService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Transactional
    public void crearVerificacion(Usuario usuario) {
        emailVerificationTokenRepository.deleteByUsuarioId(usuario.getId());

        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        String tokenReal = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setTokenHash(generarHash(tokenReal));
        token.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        token.setUsuario(usuario);

        emailVerificationTokenRepository.save(token);
        emailService.enviarVerificacionEmail(usuario.getEmail(), tokenReal);
    }

    @Transactional(noRollbackFor = VerificationTokenExpiradoException.class)
    public void verificarEmail(String tokenReal) {
        String tokenHash = generarHash(tokenReal);

        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(VerificationTokenInvalidoException::new);

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            emailVerificationTokenRepository.delete(token);
            throw new VerificationTokenExpiradoException();
        }

        Usuario usuario = token.getUsuario();
        usuario.setEmailVerificado(true);

        usuarioRepository.save(usuario);
        emailVerificationTokenRepository.delete(token);
    }

    private String generarHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el hash del token",
                    exception
            );
        }
    }
}