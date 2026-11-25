package com.template.auth.foros.repository;

import com.template.auth.foros.entity.Foro;
import com.template.auth.foros.enums.ForoCategoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ForoRepository extends JpaRepository<Foro, Long> {
    long countByUsuario_Id(Long usuarioId);

    @Query("""
        SELECT f
        FROM Foro f
        WHERE f.estadoPublicacion = 'PUBLICADO'
        AND (:categoria IS NULL OR f.categoria = :categoria)
        AND (
            :texto = ''
            OR LOWER(f.titulo) LIKE CONCAT('%', :texto, '%')
            OR LOWER(f.descripcion) LIKE CONCAT('%', :texto, '%')
        )
        """)
    Page<Foro> buscarPorCategoriaYTexto(
            @Param("categoria") ForoCategoria categoria,
            @Param("texto") String texto,
            Pageable pageable
    );
}
