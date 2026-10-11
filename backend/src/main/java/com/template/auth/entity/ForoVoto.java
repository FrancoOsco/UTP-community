package com.template.auth.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="foro_votos",uniqueConstraints=@UniqueConstraint(name="uk_voto_publicacion_usuario",columnNames={"publicacion_id","usuario_id"}))
@Getter @Setter @NoArgsConstructor
public class ForoVoto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="publicacion_id") private ForoPublicacion publicacion;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="usuario_id") private Usuario usuario;
    @Column(nullable=false) private int valor;
}
