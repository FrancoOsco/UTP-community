package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.auth.foros.entity.ComentarioForo;

import java.util.List;

public interface ComentarioForoRepository extends JpaRepository<ComentarioForo, Long> {
    long countByUsuario_Id(Long usuarioId);

    long countByForo_ForoIdAndEstadoPublicacionAndEstadoModeracion(
            Long foroId,
            String estadoPublicacion,
            String estadoModeracion
    );

    List<ComentarioForo> findByForo_ForoIdAndEstadoPublicacionAndEstadoModeracionOrderByFechaCreacionAsc(
            Long foroId,
            String estadoPublicacion,
            String estadoModeracion
    );

    java.util.Optional<ComentarioForo> findByComentarioIdAndEstadoPublicacionAndEstadoModeracion(
            Long comentarioId,
            String estadoPublicacion,
            String estadoModeracion
    );
}
