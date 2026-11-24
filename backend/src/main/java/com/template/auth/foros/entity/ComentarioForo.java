package com.template.auth.foros.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import com.template.auth.entity.Usuario;

@Entity
@Table(name = "comentario_foro")
@Getter
@Setter
@NoArgsConstructor
public class ComentarioForo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comentario_id")
    private Long comentarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "foro_id", nullable = false)
    private Foro foro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_padre_id")
    private ComentarioForo comentarioPadre;

    @Column(name = "contenido_texto", nullable = false, columnDefinition = "TEXT")
    private String contenidoTexto;

    @Column(name = "estado_publicacion", nullable = false, length = 20)
    private String estadoPublicacion;

    @Column(name = "estado_moderacion", nullable = false, length = 30)
    private String estadoModeracion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
        if (estadoPublicacion == null) {
            estadoPublicacion = "PUBLICADO";
        }
        if (estadoModeracion == null) {
            estadoModeracion = "PENDIENTE";
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
