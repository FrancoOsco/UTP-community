package com.template.auth.repository;
import com.template.auth.entity.ForoRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ForoRespuestaRepository extends JpaRepository<ForoRespuesta,Long> {
    long countByAutorId(Long id);
    long countByPublicacionId(Long id);
    List<ForoRespuesta> findByPublicacionIdOrderByCreadoEnAsc(Long id);
}
