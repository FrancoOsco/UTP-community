package com.template.auth.foros.entity;

import com.template.auth.entity.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "foro_votos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_voto_foro_usuario",
        columnNames = {"foro_id", "usuario_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ForoVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "foro_id", nullable = false)
    private Foro foro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "valor", nullable = false)
    private int valor;
}
