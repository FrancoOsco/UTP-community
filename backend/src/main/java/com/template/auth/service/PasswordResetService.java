package com.template.auth.service;

import com.template.auth.entity.PasswordResetToken;
import com.template.auth.entity.Usuario;
import com.template.auth.exception.PasswordResetTokenExpiradoException;
import com.template.auth.exception.PasswordResetTokenInvalidoException;
import com.template.auth.exception.PasswordReutilizadoException;
import com.template.auth.repository.PasswordResetTokenRepository;
import com.template.auth.repository.RefreshTokenRepository;
import com.template.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityNotificationService securityNotificationService;

    @Transactional
    public void solicitarRecuperacion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setTokenHash(generarHash(token));
        passwordResetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        passwordResetToken.setUsuario(usuario);

        passwordResetTokenRepository.save(passwordResetToken);

        try {
            emailService.enviarRecuperacionPassword(
                    usuario.getEmail(),
                    token
            );
        } catch (MailException exception) {
            log.error(
                    "No se pudo confirmar correctamente el envío del correo de recuperación a {}",
                    usuario.getEmail(),
                    exception
            );
        }
    }

    @Transactional(noRollbackFor = PasswordResetTokenExpiradoException.class)
    public void restablecerPassword(String token, String nuevaPassword) {
        String tokenHash = generarHash(token);

        PasswordResetToken passwordResetToken =
                passwordResetTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(
                                PasswordResetTokenInvalidoException::new
                        );

        if (passwordResetToken
                .getFechaExpiracion()
                .isBefore(LocalDateTime.now())) {

            passwordResetTokenRepository.delete(
                    passwordResetToken
            );

            throw new PasswordResetTokenExpiradoException();
        }

        Usuario usuario =
                passwordResetToken.getUsuario();

        if (passwordEncoder.matches(
                nuevaPassword,
                usuario.getPassword()
        )) {
            throw new PasswordReutilizadoException();
        }

        usuario.setPassword(
                passwordEncoder.encode(nuevaPassword)
        );

        usuarioRepository.save(usuario);

        refreshTokenRepository.deleteByUsuarioId(
                usuario.getId()
        );

        passwordResetTokenRepository.delete(
                passwordResetToken
        );

        securityNotificationService
                .notificarCambioPassword(
                        usuario.getEmail()
                );
    }

    private String generarHash(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el hash del token",
                    exception
            );
        }
    }
}