package com.template.auth.foros.service;

import java.math.BigDecimal;
import java.util.List;

import com.template.auth.auth.entity.Usuario;
import com.template.auth.auth.enums.UsuarioRol;
import com.template.auth.auth.repository.UsuarioRepository;
import com.template.auth.foros.dto.ModeracionAsignacionResponse;
import com.template.auth.foros.dto.ModeracionBandejaResponse;
import com.template.auth.foros.dto.ModeracionResolucionResponse;
import com.template.auth.foros.entity.ComentarioForo;
import com.template.auth.foros.entity.Foro;
import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.enums.DecisionFinalModerador;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.repository.ModeracionRepository;
import com.template.auth.integration.moderation.ModerationApiClient;
import com.template.auth.integration.moderation.dto.ModerationResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModeracionService {

        private final ModerationApiClient moderationApiClient;
        private final ModeracionRepository moderacionRepository;
        private final UsuarioRepository usuarioRepository;
        private final ForoModeracionEstadoService foroEstadoService;
        private final ComentarioModeracionEstadoService comentarioEstadoService;

        public ModeracionService(
                        ModerationApiClient moderationApiClient,
                        ModeracionRepository moderacionRepository,
                        UsuarioRepository usuarioRepository,
                        ForoModeracionEstadoService foroEstadoService,
                        ComentarioModeracionEstadoService comentarioEstadoService) {
                this.moderationApiClient = moderationApiClient;
                this.moderacionRepository = moderacionRepository;
                this.usuarioRepository = usuarioRepository;
                this.foroEstadoService = foroEstadoService;
                this.comentarioEstadoService = comentarioEstadoService;
        }

        /** Registra la decisión humana y actualiza el contenido en una transacción. */
        @Transactional
        public ModeracionResolucionResponse resolverRevision(
                        String email,
                        Long moderacionId,
                        DecisionFinalModerador decisionFinal,
                        String motivo) {
                Usuario moderador = obtenerModerador(email);
                if (decisionFinal == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "La decisión final es obligatoria.");
                }

                Moderacion revision = moderacionRepository.findById(moderacionId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "La revisión no existe."));
                if (revision.getDecision() != DecisionModeracion.REQUIERE_REVISION) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión ya fue resuelta o dejó de estar disponible.");
                }

                boolean esForo = revision.getForoId() != null && revision.getComentarioId() == null;
                boolean esComentario = revision.getComentarioId() != null && revision.getForoId() == null;
                if (!esForo && !esComentario) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión no tiene un tipo de contenido válido.");
                }

                DecisionModeracion decision = DecisionModeracion.valueOf(decisionFinal.name());
                short etiquetaConfirmada = decisionFinal == DecisionFinalModerador.APROBADO
                                ? (short) 0
                                : (short) 1;
                String motivoNormalizado = motivo == null || motivo.isBlank()
                                ? null
                                : motivo.trim();

                int actualizadas = moderacionRepository.resolverRevision(
                                moderacionId,
                                moderador.getId(),
                                decision.name(),
                                etiquetaConfirmada,
                                motivoNormalizado);
                if (actualizadas != 1) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La reserva venció, pertenece a otro moderador o la revisión dejó de estar disponible.");
                }

                if (esForo) {
                        foroEstadoService.actualizarEstado(revision.getForoId(), decision);
                } else {
                        comentarioEstadoService.actualizarEstado(revision.getComentarioId(), decision);
                }

                Moderacion resuelta = moderacionRepository.findById(moderacionId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "La revisión dejó de existir."));
                return new ModeracionResolucionResponse(
                                "Revisión resuelta correctamente.",
                                resuelta.getModeracionId(),
                                esForo ? "FORO" : "COMENTARIO",
                                resuelta.getForoId(),
                                resuelta.getComentarioId(),
                                resuelta.getDecision(),
                                resuelta.getEtiquetaConfirmada(),
                                resuelta.getRevisadoPor(),
                                resuelta.getFechaRevision(),
                                resuelta.getMotivoDecision());
        }

        /**
         * Reserva una revisión para el moderador autenticado. La operación de
         * actualización en el repositorio es atómica para evitar dobles reservas.
         */
        @Transactional
        public ModeracionAsignacionResponse tomarRevision(String email, Long moderacionId) {
                Usuario moderador = obtenerModerador(email);
                Moderacion revision = moderacionRepository.findById(moderacionId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "La revisión no existe."));

                if (revision.getDecision() != DecisionModeracion.REQUIERE_REVISION) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión ya no está disponible.");
                }

                int actualizadas;
                if (revision.getForoId() != null && revision.getComentarioId() == null) {
                        actualizadas = moderacionRepository.tomarRevisionForo(
                                        moderacionId,
                                        moderador.getId());
                } else if (revision.getComentarioId() != null && revision.getForoId() == null) {
                        actualizadas = moderacionRepository.tomarRevisionComentario(
                                        moderacionId,
                                        moderador.getId());
                } else {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión no tiene un tipo de contenido válido.");
                }

                if (actualizadas != 1) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión ya fue tomada, venció o dejó de estar disponible.");
                }

                // Las consultas de reserva limpian el contexto de persistencia; se
                // vuelve a leer el registro para devolver sus datos actualizados.
                Moderacion revisionActualizada = moderacionRepository
                                .findWithContentAndAssigneeByModeracionId(moderacionId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "La revisión dejó de existir."));
                return new ModeracionAsignacionResponse(
                                "Revisión asignada correctamente.",
                                aRespuesta(revisionActualizada));
        }

        /** Libera una revisión únicamente cuando pertenece al moderador autenticado. */
        @Transactional
        public void liberarRevision(String email, Long moderacionId) {
                Usuario moderador = obtenerModerador(email);
                moderacionRepository.findById(moderacionId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "La revisión no existe."));

                int actualizadas = moderacionRepository.liberarRevision(
                                moderacionId,
                                moderador.getId());
                if (actualizadas != 1) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "La revisión ya no está asignada a este moderador o dejó de estar disponible.");
                }
        }

        /** Devuelve la bandeja compartida de revisiones que requieren intervención. */
        @Transactional(readOnly = true)
        public List<ModeracionBandejaResponse> obtenerBandeja(String email) {
                obtenerModerador(email);
                return moderacionRepository.obtenerBandeja(DecisionModeracion.REQUIERE_REVISION)
                                .stream()
                                .map(this::aRespuesta)
                                .toList();
        }

        /** Devuelve las reservas vigentes del moderador autenticado. */
        @Transactional(readOnly = true)
        public List<ModeracionBandejaResponse> obtenerMisRevisiones(String email) {
                Usuario moderador = obtenerModerador(email);
                return moderacionRepository.obtenerMisRevisiones(
                                moderador.getId(),
                                DecisionModeracion.REQUIERE_REVISION)
                                .stream()
                                .map(this::aRespuesta)
                                .toList();
        }

        private ModeracionBandejaResponse aRespuesta(Moderacion moderacion) {
                boolean esForo = moderacion.getForoId() != null;
                Foro foro = esForo ? moderacion.getForo() : null;
                ComentarioForo comentario = esForo ? null : moderacion.getComentarioForo();

                String categoria = esForo && foro.getCategoria() != null
                                ? foro.getCategoria().name()
                                : null;
                String titulo = esForo ? foro.getTitulo() : null;
                String contenido = esForo
                                ? foro.getDescripcion()
                                : comentario.getContenidoTexto();
                Usuario autor = esForo ? foro.getUsuario() : comentario.getUsuario();
                String nombreAutor = (autor.getNombre() + " " + autor.getApellido()).trim();

                Usuario moderadorAsignado = moderacion.getModeradorAsignado();
                String nombreModerador = moderadorAsignado == null
                                ? null
                                : (moderadorAsignado.getNombre() + " " + moderadorAsignado.getApellido()).trim();

                return new ModeracionBandejaResponse(
                                moderacion.getModeracionId(),
                                esForo ? "FORO" : "COMENTARIO",
                                moderacion.getForoId(),
                                moderacion.getComentarioId(),
                                categoria,
                                titulo,
                                contenido,
                                autor.getId(),
                                nombreAutor,
                                moderacion.getEtiquetaPredicha(),
                                moderacion.getModerationScore(),
                                moderacion.getPredictionConfidence(),
                                moderacion.getDecision(),
                                moderacion.getModeloNombre(),
                                moderacion.getModeloVersion(),
                                moderacion.getFechaEvaluacion(),
                                moderacion.getAtendidoPor(),
                                nombreModerador,
                                moderacion.getFechaAsignacion(),
                                moderacion.getFechaExpiracionAsignacion());
        }

        private Usuario obtenerModerador(String email) {
                if (email == null || email.isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Se requiere un usuario autenticado.");
                }

                Usuario usuario = usuarioRepository.findByEmail(email)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "El usuario autenticado no existe."));

                if (!UsuarioRol.MODERADOR.equals(usuario.getRol())
                                && !UsuarioRol.ADMIN.equals(usuario.getRol())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "El usuario no tiene permisos para moderar contenido.");
                }

                return usuario;
        }

        // Evaluar una publicación de foro
        public Moderacion evaluarForo(Long foroId, String texto) {

                validarContenido(foroId, null);

                ModerationResponse prediccion = moderationApiClient.predict(texto);

                return guardarModeracion(
                                foroId,
                                null,
                                prediccion);
        }

        // Evaluar un comentario
        public Moderacion evaluarComentario(
                        Long comentarioId,
                        String texto) {

                validarContenido(null, comentarioId);

                ModerationResponse prediccion = moderationApiClient.predict(texto);

                return guardarModeracion(
                                null,
                                comentarioId,
                                prediccion);
        }

        // Construir y persistir la moderación
        private Moderacion guardarModeracion(
                        Long foroId,
                        Long comentarioId,
                        ModerationResponse prediccion) {

                validarPrediccion(prediccion);

                double score = prediccion.score();

                DecisionModeracion decision = determinarDecision(score);

                Moderacion moderacion = new Moderacion();

                moderacion.setForoId(foroId);
                moderacion.setComentarioId(comentarioId);

                moderacion.setModeloNombre(
                                prediccion.modelName());

                moderacion.setModeloVersion(
                                prediccion.modelVersion());

                moderacion.setEtiquetaPredicha(
                                prediccion.label().shortValue());

                moderacion.setModerationScore(
                                BigDecimal.valueOf(score));

                moderacion.setPredictionConfidence(
                                BigDecimal.valueOf(prediccion.probability()));

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
                        Long comentarioId) {

                boolean foroPresente = foroId != null;
                boolean comentarioPresente = comentarioId != null;

                if (foroPresente == comentarioPresente) {
                        throw new IllegalArgumentException(
                                        "La moderación debe corresponder a un foro "
                                                        + "o a un comentario, pero no a ambos.");
                }
        }

        // Validar la respuesta de Python
        private void validarPrediccion(
                        ModerationResponse prediccion) {

                if (prediccion == null
                                || prediccion.label() == null
                                || prediccion.probability() == null
                                || prediccion.score() == null
                                || prediccion.modelVersion() == null
                                || prediccion.modelName() == null) {

                        throw new IllegalArgumentException(
                                        "La predicción recibida está incompleta.");
                }

                Integer label = prediccion.label();
                double score = prediccion.score();
                double probability = prediccion.probability();

                if (label != 0 && label != 1) {
                        throw new IllegalArgumentException(
                                        "La etiqueta predicha debe ser 0 o 1.");
                }

                if (!Double.isFinite(score)
                                || score < 0 || score > 1) {

                        throw new IllegalArgumentException(
                                        "El score debe estar entre 0 y 1.");
                }

                if (!Double.isFinite(probability)
                                || probability < 0 || probability > 1) {

                        throw new IllegalArgumentException(
                                        "La confianza debe estar entre 0 y 1.");
                }

                if (prediccion.modelVersion().isBlank()
                                || prediccion.modelName().isBlank()) {

                        throw new IllegalArgumentException(
                                        "La identificación del modelo no puede estar vacía.");
                }
        }

        public ModerationResponse predecir(String texto) {

                ModerationResponse prediccion = moderationApiClient.predict(texto);

                validarPrediccion(prediccion);

                return prediccion;
        }

        public Moderacion registrarPrediccionForo(
                        Long foroId,
                        ModerationResponse prediccion) {
                validarContenido(foroId, null);

                return guardarModeracion(
                                foroId,
                                null,
                                prediccion);
        }

        public Moderacion registrarPrediccionComentario(
                        Long comentarioId,
                        ModerationResponse prediccion) {
                validarContenido(null, comentarioId);

                return guardarModeracion(
                                null,
                                comentarioId,
                                prediccion);
        }
}
