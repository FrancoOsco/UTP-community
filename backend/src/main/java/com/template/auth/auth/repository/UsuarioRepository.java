package com.template.auth.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.auth.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
