package com.template.auth.foros.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.template.auth.foros.entity.ForoVoto;

import java.util.Optional;

public interface ForoVotoRepository extends JpaRepository<ForoVoto, Long> {
    Optional<ForoVoto> findByForo_ForoIdAndUsuario_Id(Long foroId, Long usuarioId);

    @Query("select coalesce(sum(v.valor), 0) from ForoVoto v where v.foro.foroId = :foroId")
    Long sumarValorPorForoId(@Param("foroId") Long foroId);
}
