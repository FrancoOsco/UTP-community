package com.template.auth.foros.repository;

import com.template.auth.foros.entity.ForoCategoria;
import com.template.auth.foros.entity.Foro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForoRepository extends JpaRepository<Foro, Long> {
    long countByUsuario_Id(Long usuarioId);

    @Query("select f from Foro f where (:categoria is null or f.categoria = :categoria) and (:texto = '' or lower(f.titulo) like concat('%', :texto, '%') or lower(f.descripcion) like concat('%', :texto, '%'))")
    Page<Foro> buscarPorCategoriaYTexto(@Param("categoria") ForoCategoria categoria, @Param("texto") String texto, Pageable pageable);
}
