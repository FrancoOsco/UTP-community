package com.template.auth.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.auth.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUsuarioId(Long usuarioId);
}
