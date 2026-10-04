package com.template.auth.service;

import com.template.auth.entity.Usuario;
import com.template.auth.repository.EmailVerificationTokenRepository;
import com.template.auth.repository.PasswordResetTokenRepository;
import com.template.auth.repository.RefreshTokenRepository;
import com.template.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void debeEliminarUsuarioYTokensRelacionados() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.eliminarUsuario(1L);

        verify(emailVerificationTokenRepository).deleteByUsuarioId(1L);
        verify(passwordResetTokenRepository).deleteByUsuarioId(1L);
        verify(refreshTokenRepository).deleteByUsuarioId(1L);
        verify(usuarioRepository).delete(usuario);
    }
}