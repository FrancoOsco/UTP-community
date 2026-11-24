package com.template.auth.foros.entity;

import com.template.auth.entity.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "comentario_votos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_voto_comentario_usuario",
        columnNames = {"comentario_id", "usuario_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ComentarioVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comentario_id", nullable = false)
    private ComentarioForo comentario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "valor", nullable = false)
    private int valor;
}
