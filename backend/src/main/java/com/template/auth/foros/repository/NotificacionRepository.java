package com.template.auth.foros.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.template.auth.foros.entity.Notificacion;

@Repository
public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long> {

    // Consultar todas las notificaciones de un usuario.
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(
        Long usuarioId
    );

    // Consultar notificaciones no leídas.
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(
        Long usuarioId
    );

    // Contar notificaciones no leídas.
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // Generar una notificación para cada moderador y administrador.
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO notificacion (
            usuario_id,
            moderacion_id,
            tipo_contenido,
            contenido_id,
            mensaje,
            leida,
            fecha_creacion
        )
        SELECT
            u.id,
            m.moderacion_id,
            CASE
                WHEN m.foro_id IS NOT NULL THEN 'FORO'
                ELSE 'COMENTARIO'
            END,
            COALESCE(m.foro_id, m.comentario_id),
            CASE
                WHEN m.foro_id IS NOT NULL
                    THEN 'Una publicación requiere revisión humana.'
                ELSE 'Un comentario requiere revisión humana.'
            END,
            FALSE,
            CURRENT_TIMESTAMP
        FROM moderacion m
        CROSS JOIN usuario u
        WHERE m.moderacion_id = :moderacionId
          AND m.decision = 'REQUIERE_REVISION'
          AND u.rol IN ('MODERADOR', 'ADMIN')
        ON CONFLICT (usuario_id, moderacion_id)
        DO NOTHING
        """, nativeQuery = true)
    int generarParaModeracion(
        @Param("moderacionId") Long moderacionId
    );

    // Marcar como leída, comprobando que pertenece al usuario.
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE notificacion
        SET leida = TRUE,
            fecha_lectura = CURRENT_TIMESTAMP
        WHERE notificacion_id = :notificacionId
          AND usuario_id = :usuarioId
          AND leida = FALSE
        """, nativeQuery = true)
    int marcarComoLeida(
        @Param("notificacionId") Long notificacionId,
        @Param("usuarioId") Long usuarioId
    );
}