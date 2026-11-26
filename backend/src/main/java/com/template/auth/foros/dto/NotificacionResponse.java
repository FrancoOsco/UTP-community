package com.template.auth.foros.dto;

import java.time.OffsetDateTime;

public record NotificacionResponse(
        Long notificacionId,
        Long moderacionId,
        String tipoContenido,
        Long contenidoId,
        String mensaje,
        boolean leida,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaLectura
) {}