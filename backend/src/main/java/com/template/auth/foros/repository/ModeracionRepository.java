package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.enums.DecisionModeracion;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModeracionRepository
        extends JpaRepository<Moderacion, Long> {

    @EntityGraph(attributePaths = {"foro", "foro.usuario", "comentarioForo", "comentarioForo.usuario", "moderadorAsignado"})
    Optional<Moderacion> findWithContentAndAssigneeByModeracionId(Long moderacionId);

    // Consultar moderaciones de un foro
    List<Moderacion> findByForoIdOrderByFechaEvaluacionDesc(
            Long foroId
    );

    // Consultar moderaciones de un comentario
    List<Moderacion> findByComentarioIdOrderByFechaEvaluacionDesc(
            Long comentarioId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE moderacion m
        SET atendido_por = :moderadorId,
                fecha_asignacion = CURRENT_TIMESTAMP,
                fecha_expiracion_asignacion =
                CURRENT_TIMESTAMP + INTERVAL '30 minutes'
        WHERE m.moderacion_id = :moderacionId
        AND m.foro_id IS NOT NULL
        AND m.comentario_id IS NULL
        AND m.decision = 'REQUIERE_REVISION'
        AND (
                m.atendido_por IS NULL
                OR m.fecha_expiracion_asignacion <= CURRENT_TIMESTAMP
        )
        AND EXISTS (
                SELECT 1
                FROM foro f
                WHERE f.foro_id = m.foro_id
                AND f.estado_moderacion = 'REQUIERE_REVISION'
                AND f.estado_publicacion = 'OCULTO'
        )
        AND NOT EXISTS (
                SELECT 1
                FROM moderacion posterior
                WHERE posterior.foro_id = m.foro_id
                AND posterior.moderacion_id > m.moderacion_id
        )
        """, nativeQuery = true)
        int tomarRevisionForo(
        @Param("moderacionId") Long moderacionId,
        @Param("moderadorId") Long moderadorId
        );

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query(value = """
        UPDATE moderacion m
        SET atendido_por = :moderadorId,
                fecha_asignacion = CURRENT_TIMESTAMP,
                fecha_expiracion_asignacion =
                CURRENT_TIMESTAMP + INTERVAL '30 minutes'
        WHERE m.moderacion_id = :moderacionId
        AND m.comentario_id IS NOT NULL
        AND m.foro_id IS NULL
        AND m.decision = 'REQUIERE_REVISION'
        AND (
                m.atendido_por IS NULL
                OR m.fecha_expiracion_asignacion <= CURRENT_TIMESTAMP
        )
        AND EXISTS (
                SELECT 1
                FROM comentario_foro c
                WHERE c.comentario_id = m.comentario_id
                AND c.estado_moderacion = 'REQUIERE_REVISION'
                AND c.estado_publicacion = 'OCULTO'
        )
        AND NOT EXISTS (
                SELECT 1
                FROM moderacion posterior
                WHERE posterior.comentario_id = m.comentario_id
                AND posterior.moderacion_id > m.moderacion_id
        )
        """, nativeQuery = true)
        int tomarRevisionComentario(
        @Param("moderacionId") Long moderacionId,
        @Param("moderadorId") Long moderadorId
        );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE moderacion m
        SET decision = :decision,
            etiqueta_confirmada = :etiquetaConfirmada,
            revisado_por = :moderadorId,
            fecha_revision = CURRENT_TIMESTAMP,
            motivo_decision = :motivo,
            atendido_por = NULL,
            fecha_asignacion = NULL,
            fecha_expiracion_asignacion = NULL
        WHERE m.moderacion_id = :moderacionId
          AND m.decision = 'REQUIERE_REVISION'
          AND m.atendido_por = :moderadorId
          AND m.fecha_expiracion_asignacion > CURRENT_TIMESTAMP
          AND (
                (
                    m.foro_id IS NOT NULL
                    AND m.comentario_id IS NULL
                    AND EXISTS (
                        SELECT 1 FROM foro f
                        WHERE f.foro_id = m.foro_id
                          AND f.estado_moderacion = 'REQUIERE_REVISION'
                          AND f.estado_publicacion = 'OCULTO'
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM moderacion posterior
                        WHERE posterior.foro_id = m.foro_id
                          AND posterior.moderacion_id > m.moderacion_id
                    )
                )
                OR
                (
                    m.comentario_id IS NOT NULL
                    AND m.foro_id IS NULL
                    AND EXISTS (
                        SELECT 1 FROM comentario_foro c
                        WHERE c.comentario_id = m.comentario_id
                          AND c.estado_moderacion = 'REQUIERE_REVISION'
                          AND c.estado_publicacion = 'OCULTO'
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM moderacion posterior
                        WHERE posterior.comentario_id = m.comentario_id
                          AND posterior.moderacion_id > m.moderacion_id
                    )
                )
          )
        """, nativeQuery = true)
    int resolverRevision(
        @Param("moderacionId") Long moderacionId,
        @Param("moderadorId") Long moderadorId,
        @Param("decision") String decision,
        @Param("etiquetaConfirmada") Short etiquetaConfirmada,
        @Param("motivo") String motivo
    );

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query(value = """
        UPDATE moderacion
        SET atendido_por = NULL,
                fecha_asignacion = NULL,
                fecha_expiracion_asignacion = NULL
        WHERE moderacion_id = :moderacionId
        AND atendido_por = :moderadorId
        AND decision = 'REQUIERE_REVISION'
        """, nativeQuery = true)
        int liberarRevision(
        @Param("moderacionId") Long moderacionId,
        @Param("moderadorId") Long moderadorId
        );

        // Bandeja compartida: revisiones disponibles, asignadas y vencidas.
        @EntityGraph(attributePaths = {"foro", "foro.usuario", "comentarioForo", "comentarioForo.usuario", "moderadorAsignado"})
        @Query("""
        SELECT m
        FROM Moderacion m
        LEFT JOIN FETCH m.foro
        LEFT JOIN FETCH m.comentarioForo
        LEFT JOIN FETCH m.moderadorAsignado
        WHERE m.decision = :decision
        AND (
                (
                m.foroId IS NOT NULL
                AND m.moderacionId = (
                        SELECT MAX(m2.moderacionId)
                        FROM Moderacion m2
                        WHERE m2.foroId = m.foroId
                )
                AND EXISTS (
                        SELECT f.foroId
                        FROM Foro f
                        WHERE f.foroId = m.foroId
                        AND f.estadoModeracion = 'REQUIERE_REVISION'
                        AND f.estadoPublicacion = 'OCULTO'
                )
                )
                OR
                (
                m.comentarioId IS NOT NULL
                AND m.moderacionId = (
                        SELECT MAX(m2.moderacionId)
                        FROM Moderacion m2
                        WHERE m2.comentarioId = m.comentarioId
                )
                AND EXISTS (
                        SELECT c.comentarioId
                        FROM ComentarioForo c
                        WHERE c.comentarioId = m.comentarioId
                        AND c.estadoModeracion = 'REQUIERE_REVISION'
                        AND c.estadoPublicacion = 'OCULTO'
                )
                )
        )
        ORDER BY m.fechaEvaluacion ASC
        """)
        List<Moderacion> obtenerBandeja(
        @Param("decision") DecisionModeracion decision
        );

        // Revisiones asignadas al moderador autenticado y cuya reserva sigue vigente.
        @EntityGraph(attributePaths = {"foro", "foro.usuario", "comentarioForo", "comentarioForo.usuario", "moderadorAsignado"})
        @Query("""
        SELECT m
        FROM Moderacion m
        LEFT JOIN FETCH m.foro
        LEFT JOIN FETCH m.comentarioForo
        LEFT JOIN FETCH m.moderadorAsignado
        WHERE m.atendidoPor = :moderadorId
        AND m.decision = :decision
        AND m.fechaExpiracionAsignacion > CURRENT_TIMESTAMP
        AND (
                (
                m.foroId IS NOT NULL
                AND m.moderacionId = (
                        SELECT MAX(m2.moderacionId)
                        FROM Moderacion m2
                        WHERE m2.foroId = m.foroId
                )
                AND EXISTS (
                        SELECT f.foroId
                        FROM Foro f
                        WHERE f.foroId = m.foroId
                        AND f.estadoModeracion = 'REQUIERE_REVISION'
                        AND f.estadoPublicacion = 'OCULTO'
                )
                )
                OR
                (
                m.comentarioId IS NOT NULL
                AND m.moderacionId = (
                        SELECT MAX(m2.moderacionId)
                        FROM Moderacion m2
                        WHERE m2.comentarioId = m.comentarioId
                )
                AND EXISTS (
                        SELECT c.comentarioId
                        FROM ComentarioForo c
                        WHERE c.comentarioId = m.comentarioId
                        AND c.estadoModeracion = 'REQUIERE_REVISION'
                        AND c.estadoPublicacion = 'OCULTO'
                )
                )
        )
        ORDER BY m.fechaAsignacion ASC
        """)
        List<Moderacion> obtenerMisRevisiones(
        @Param("moderadorId") Long moderadorId,
        @Param("decision") DecisionModeracion decision
        );
}
