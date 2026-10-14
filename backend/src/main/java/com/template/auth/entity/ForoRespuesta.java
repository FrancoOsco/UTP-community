package com.template.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "foro_respuestas")
@Getter
@Setter
@NoArgsConstructor
public class ForoRespuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id")
    private ForoPublicacion publicacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;
    @Column(nullable = false, length = 3000)
    private String contenido;
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void antes() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
