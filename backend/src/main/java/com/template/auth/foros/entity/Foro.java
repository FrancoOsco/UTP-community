package com.template.auth.foros.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import com.template.auth.auth.entity.Usuario;
import com.template.auth.foros.enums.ForoCategoria;

@Entity
@Table(name = "foro")
@Getter
@Setter
@NoArgsConstructor
public class Foro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "foro_id")
    private Long foroId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 50)
    private ForoCategoria categoria;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "estado_publicacion", nullable = false, length = 20)
    private String estadoPublicacion;

    @Column(name = "estado_moderacion", nullable = false, length = 30)
    private String estadoModeracion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_disponible")
    private OffsetDateTime fechaDisponible;

    @Column(name = "fecha_vencimiento")
    private OffsetDateTime fechaVencimiento;

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
            estadoPublicacion = "BORRADOR";
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