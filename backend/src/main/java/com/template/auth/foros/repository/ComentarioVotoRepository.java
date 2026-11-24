package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.template.auth.foros.entity.ComentarioVoto;

import java.util.Optional;

public interface ComentarioVotoRepository extends JpaRepository<ComentarioVoto, Long> {
    Optional<ComentarioVoto> findByComentario_ComentarioIdAndUsuario_Id(Long comentarioId, Long usuarioId);

    @Query("select coalesce(sum(v.valor), 0) from ComentarioVoto v where v.comentario.comentarioId = :comentarioId")
    Long sumarValorPorComentarioId(@Param("comentarioId") Long comentarioId);
}
