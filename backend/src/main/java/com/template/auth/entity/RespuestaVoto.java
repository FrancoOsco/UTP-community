package com.template.auth.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="respuesta_votos",uniqueConstraints=@UniqueConstraint(name="uk_voto_respuesta_usuario",columnNames={"respuesta_id","usuario_id"}))
@Getter @Setter @NoArgsConstructor
public class RespuestaVoto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="respuesta_id") private ForoRespuesta respuesta;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="usuario_id") private Usuario usuario;
    @Column(nullable=false) private int valor;
}
