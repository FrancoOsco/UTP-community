package com.template.auth.repository;
import com.template.auth.entity.RespuestaVoto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface RespuestaVotoRepository extends JpaRepository<RespuestaVoto,Long> {
    Optional<RespuestaVoto> findByRespuestaIdAndUsuarioId(Long respuestaId,Long usuarioId);
    @Query("select coalesce(sum(v.valor),0) from RespuestaVoto v where v.respuesta.id=:id")
    Long puntaje(@Param("id") Long id);
}
