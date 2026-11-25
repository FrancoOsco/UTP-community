package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.template.auth.foros.entity.Moderacion;

import java.util.List;

@Repository
public interface ModeracionRepository
        extends JpaRepository<Moderacion, Long> {

    // Consultar moderaciones de un foro
    List<Moderacion> findByForoIdOrderByFechaEvaluacionDesc(
            Long foroId
    );

    // Consultar moderaciones de un comentario
    List<Moderacion> findByComentarioIdOrderByFechaEvaluacionDesc(
            Long comentarioId
    );
}