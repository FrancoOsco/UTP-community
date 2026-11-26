package com.template.auth.foros.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.template.auth.foros.enums.DecisionModeracion;

public record ModeracionBandejaResponse(
        Long moderacionId,
        String tipoContenido,
        Long foroId,
        Long comentarioId,
        String categoria,
        String titulo,
        String contenido,
        Long autorId,
        String autorNombre,
        Short etiquetaPredicha,
        BigDecimal moderationScore,
        BigDecimal predictionConfidence,
        DecisionModeracion decision,
        String modeloNombre,
        String modeloVersion,
        OffsetDateTime fechaEvaluacion,
        Long atendidoPor,
        String moderadorAsignado,
        OffsetDateTime fechaAsignacion,
        OffsetDateTime fechaExpiracionAsignacion
) {
}
