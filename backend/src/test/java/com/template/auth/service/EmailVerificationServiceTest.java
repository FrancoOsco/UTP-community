package com.template.auth.service;

import com.template.auth.exception.VerificationTokenInvalidoException;
import com.template.auth.repository.EmailVerificationTokenRepository;
import com.template.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void debeRechazarTokenDeVerificacionInvalido() {
        when(emailVerificationTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                VerificationTokenInvalidoException.class,
                () -> emailVerificationService.verificarEmail("token-invalido")
        );

        verify(emailVerificationTokenRepository).findByTokenHash(anyString());
        verify(usuarioRepository, never()).save(any());
    }
}
