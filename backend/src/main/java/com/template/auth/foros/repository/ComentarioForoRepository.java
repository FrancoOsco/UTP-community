package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.foros.entity.ComentarioForo;

import java.util.List;

public interface ComentarioForoRepository extends JpaRepository<ComentarioForo, Long> {
    long countByUsuario_Id(Long usuarioId);

    long countByForo_ForoId(Long foroId);

    List<ComentarioForo> findByForo_ForoIdOrderByFechaCreacionAsc(Long foroId);
}
