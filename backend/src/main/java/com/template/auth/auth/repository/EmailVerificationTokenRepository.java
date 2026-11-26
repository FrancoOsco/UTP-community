package com.template.auth.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.auth.entity.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteByUsuarioId(Long usuarioId);
}
