package com.template.auth.auth.service;

import com.template.auth.auth.dto.UsuarioResponse;
import com.template.auth.auth.entity.Usuario;
import com.template.auth.auth.exception.UsuarioNoEncontradoException;
import com.template.auth.auth.repository.EmailVerificationTokenRepository;
import com.template.auth.auth.repository.PasswordResetTokenRepository;
import com.template.auth.auth.repository.RefreshTokenRepository;
import com.template.auth.auth.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.template.auth.academic.StorageService academicStorage;
    private final org.springframework.jdbc.core.JdbcTemplate academicJdbc;

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository
                .findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        emailVerificationTokenRepository.deleteByUsuarioId(id);
        passwordResetTokenRepository.deleteByUsuarioId(id);
        refreshTokenRepository.deleteByUsuarioId(id);
        academicStorage.deleteUserFilesAfterCommit(academicJdbc,id);
        usuarioRepository.delete(usuario);
    }

    private UsuarioResponse convertirAResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }
}
