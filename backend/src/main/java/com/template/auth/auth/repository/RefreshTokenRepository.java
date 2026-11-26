package com.template.auth.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUsuarioId(Long usuarioId);
}
