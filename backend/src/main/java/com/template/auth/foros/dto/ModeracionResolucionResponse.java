package com.template.auth.foros.dto;

import java.time.OffsetDateTime;

import com.template.auth.foros.enums.DecisionModeracion;

public record ModeracionResolucionResponse(
        String mensaje,
        Long moderacionId,
        String tipoContenido,
        Long foroId,
        Long comentarioId,
        DecisionModeracion decision,
        Short etiquetaConfirmada,
        Long revisadoPor,
        OffsetDateTime fechaRevision,
        String motivoDecision
) {
}
