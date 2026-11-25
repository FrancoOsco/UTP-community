package com.template.auth.foros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.template.auth.entity.Usuario;
import com.template.auth.foros.enums.DecisionModeracion;

import lombok.*;

@Entity
@Table(name = "moderacion")
@Getter
@Setter
@NoArgsConstructor
public class Moderacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "moderacion_id", nullable = false)
    private Long moderacionId;

    @Column(name = "foro_id")
    private Long foroId;

    @Column(name = "comentario_id")
    private Long comentarioId;

    @Column(name = "modelo_nombre", nullable = false, length = 100)
    private String modeloNombre;

    @Column(name = "modelo_version", nullable = false, length = 50)
    private String modeloVersion;

    @Column(name = "etiqueta_predicha", nullable = false)
    private Short etiquetaPredicha;

    @Column(name = "moderation_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal moderationScore;

    @Column(name = "prediction_confidence", nullable = false, precision = 6, scale = 5)
    private BigDecimal predictionConfidence;

    @Column(name = "etiqueta_confirmada")
    private Short etiquetaConfirmada;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private DecisionModeracion decision;

    @Column(name = "motivo_decision", columnDefinition = "TEXT")
    private String motivoDecision;

    @Column(name = "revisado_por")
    private Long revisadoPor;

    @Column(name = "fecha_evaluacion", nullable = false, updatable = false)
    private OffsetDateTime fechaEvaluacion;

    @Column(name = "fecha_revision")
    private OffsetDateTime fechaRevision;

    // Relaciones opcionales (solo lectura/navegación si ya tienes las entidades Foro, ComentarioForo y Usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foro_id", insertable = false, updatable = false)
    private Foro foro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_id", insertable = false, updatable = false)
    private ComentarioForo comentarioForo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por", insertable = false, updatable = false)
    private Usuario revisor;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEvaluacion == null) {
            this.fechaEvaluacion = OffsetDateTime.now();
        }
        if (this.decision == null) {
            this.decision = DecisionModeracion.PENDIENTE;
        }
    }
}