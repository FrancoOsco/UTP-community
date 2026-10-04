package com.template.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityNotificationService {

    private final EmailService emailService;

    public void notificarCambioPassword(String email) {
        try {
            emailService.enviarCambioPasswordConfirmado(email);
        } catch (MailException exception) {
            log.error(
                    "No se pudo enviar la notificación de cambio de contraseña a {}",
                    email,
                    exception
            );
        }
    }
}