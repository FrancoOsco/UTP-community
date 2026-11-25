package com.template.auth.foros.service;

import java.math.BigDecimal;

import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.repository.ModeracionRepository;
import com.template.auth.integration.moderation.ModerationApiClient;
import com.template.auth.integration.moderation.dto.ModerationResponse;

import org.springframework.stereotype.Service;

@Service
public class ModeracionService {

    private final ModerationApiClient moderationApiClient;
    private final ModeracionRepository moderacionRepository;

    public ModeracionService(
            ModerationApiClient moderationApiClient,
            ModeracionRepository moderacionRepository
    ) {
        this.moderationApiClient = moderationApiClient;
        this.moderacionRepository = moderacionRepository;
    }

    // Evaluar una publicación de foro
    public Moderacion evaluarForo(Long foroId, String texto) {

        validarContenido(foroId, null);

        ModerationResponse prediccion = moderationApiClient.predict(texto);

        return guardarModeracion(
                foroId,
                null,
                prediccion
        );
    }

    // Evaluar un comentario
    public Moderacion evaluarComentario(
            Long comentarioId,
            String texto
    ) {

        validarContenido(null, comentarioId);

        ModerationResponse prediccion =
                moderationApiClient.predict(texto);

        return guardarModeracion(
                null,
                comentarioId,
                prediccion
        );
    }

    // Construir y persistir la moderación
    private Moderacion guardarModeracion(
            Long foroId,
            Long comentarioId,
            ModerationResponse prediccion
    ) {

        validarPrediccion(prediccion);

        double score = prediccion.score();

        DecisionModeracion decision =
                determinarDecision(score);

        Moderacion moderacion = new Moderacion();

        moderacion.setForoId(foroId);
        moderacion.setComentarioId(comentarioId);

        moderacion.setModeloNombre(
                prediccion.modelName()
        );

        moderacion.setModeloVersion(
                prediccion.modelVersion()
        );

        moderacion.setEtiquetaPredicha(
                prediccion.label().shortValue()
        );

        moderacion.setModerationScore(
                BigDecimal.valueOf(score)
        );

        moderacion.setPredictionConfidence(
                BigDecimal.valueOf(prediccion.probability())
        );

        moderacion.setDecision(decision);

        return moderacionRepository.save(moderacion);
    }

    // Aplicar la política de moderación
    private DecisionModeracion determinarDecision(double score) {

        if (score < 0.40) {
            return DecisionModeracion.APROBADO;
        }

        if (score < 0.90) {
            return DecisionModeracion.REQUIERE_REVISION;
        }

        return DecisionModeracion.RECHAZADO;
    }

    // Validar que se evalúe exactamente un tipo de contenido
    private void validarContenido(
            Long foroId,
            Long comentarioId
    ) {

        boolean foroPresente = foroId != null;
        boolean comentarioPresente = comentarioId != null;

        if (foroPresente == comentarioPresente) {
            throw new IllegalArgumentException(
                    "La moderación debe corresponder a un foro "
                    + "o a un comentario, pero no a ambos."
            );
        }
    }

    // Validar la respuesta de Python
    private void validarPrediccion(
            ModerationResponse prediccion
    ) {

        if (prediccion == null
                || prediccion.label() == null
                || prediccion.probability() == null
                || prediccion.score() == null
                || prediccion.modelVersion() == null
                || prediccion.modelName() == null) {

            throw new IllegalArgumentException(
                    "La predicción recibida está incompleta."
            );
        }

        Integer label = prediccion.label();
        double score = prediccion.score();
        double probability = prediccion.probability();

        if (label != 0 && label != 1) {
            throw new IllegalArgumentException(
                    "La etiqueta predicha debe ser 0 o 1."
            );
        }

        if (!Double.isFinite(score)
                || score < 0 || score > 1) {

            throw new IllegalArgumentException(
                    "El score debe estar entre 0 y 1."
            );
        }

        if (!Double.isFinite(probability)
                || probability < 0 || probability > 1) {

            throw new IllegalArgumentException(
                    "La confianza debe estar entre 0 y 1."
            );
        }

        if (prediccion.modelVersion().isBlank()
                || prediccion.modelName().isBlank()) {

            throw new IllegalArgumentException(
                    "La identificación del modelo no puede estar vacía."
            );
        }
    }
}