package com.template.auth.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="foro_publicaciones")
@Getter @Setter @NoArgsConstructor
public class ForoPublicacion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ForoCategoria categoria;
    @Column(nullable=false,length=150) private String titulo;
    @Column(nullable=false,length=5000) private String contenido;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="autor_id") private Usuario autor;
    @Column(nullable=false,updatable=false) private LocalDateTime creadoEn;
    @PrePersist void antes() { if(creadoEn==null) creadoEn=LocalDateTime.now(); }
}
