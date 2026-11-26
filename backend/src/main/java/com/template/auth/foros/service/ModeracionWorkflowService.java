package com.template.auth.foros.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.integration.moderation.dto.ModerationResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModeracionWorkflowService {

    private final ModeracionService moderacionService;
    private final ForoModeracionEstadoService foroEstadoService;
    private final ComentarioModeracionEstadoService comentarioEstadoService;
    private final NotificacionService notificacionService;

    @Transactional
    public Moderacion procesarForo(
            Long foroId,
            ModerationResponse prediccion
    ) {

        Moderacion moderacion =
                moderacionService.registrarPrediccionForo(
                        foroId,
                        prediccion
                );

        DecisionModeracion decision = moderacion.getDecision();

        foroEstadoService.actualizarEstado(foroId, decision);

        notificacionService.notificarRevision(moderacion);

        return moderacion;
    }

    @Transactional
    public Moderacion procesarComentario(
            Long comentarioId,
            ModerationResponse prediccion
    ) {

        Moderacion moderacion =
                moderacionService.registrarPrediccionComentario(
                        comentarioId,
                        prediccion
                );

        DecisionModeracion decision = moderacion.getDecision();

        comentarioEstadoService.actualizarEstado(
                comentarioId,
                decision
        );

        notificacionService.notificarRevision(moderacion);

        return moderacion;
    }
}