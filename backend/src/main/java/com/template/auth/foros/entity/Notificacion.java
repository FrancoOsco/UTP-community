package com.template.auth.foros.entity;

import java.time.OffsetDateTime;

import com.template.auth.auth.entity.Usuario;
import com.template.auth.foros.dto.TipoContenidoNotificacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(
    name = "notificacion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_notificacion_usuario_moderacion",
            columnNames = {"usuario_id", "moderacion_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long notificacionId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "moderacion_id", nullable = false)
    private Long moderacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contenido", nullable = false, length = 20)
    private TipoContenidoNotificacion tipoContenido;

    @Column(name = "contenido_id", nullable = false)
    private Long contenidoId;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private OffsetDateTime fechaLectura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderacion_id", insertable = false, updatable = false)
    private Moderacion moderacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
    }
}