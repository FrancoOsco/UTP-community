package com.template.auth.repository;

import com.template.auth.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ForoPublicacionRepository extends JpaRepository<ForoPublicacion, Long> {
    long countByAutorId(Long id);

    @Query("select p from ForoPublicacion p where (:categoria is null or p.categoria=:categoria) and (:texto='' or lower(p.titulo) like concat('%',:texto,'%') or lower(p.contenido) like concat('%',:texto,'%'))")
    Page<ForoPublicacion> buscar(@Param("categoria") ForoCategoria categoria, @Param("texto") String texto, Pageable pageable);
}
