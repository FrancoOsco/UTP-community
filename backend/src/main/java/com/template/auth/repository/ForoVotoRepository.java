package com.template.auth.repository;

import com.template.auth.entity.ForoVoto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ForoVotoRepository extends JpaRepository<ForoVoto, Long> {
    Optional<ForoVoto> findByPublicacionIdAndUsuarioId(Long publicacionId, Long usuarioId);

    @Query("select coalesce(sum(v.valor),0) from ForoVoto v where v.publicacion.id=:id")
    Long puntaje(@Param("id") Long id);
}
