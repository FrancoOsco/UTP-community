package com.template.auth.foros.service;

import com.template.auth.auth.dto.RespuestaCrearRequest;
import com.template.auth.auth.entity.Usuario;
import com.template.auth.auth.repository.UsuarioRepository;
import com.template.auth.foros.dto.ForoCrearRequest;
import com.template.auth.foros.entity.ComentarioForo;
import com.template.auth.foros.entity.ComentarioVoto;
import com.template.auth.foros.entity.Foro;
import com.template.auth.foros.entity.ForoVoto;
import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.enums.ForoCategoria;
import com.template.auth.foros.repository.ComentarioVotoRepository;
import com.template.auth.foros.repository.ComentarioForoRepository;
import com.template.auth.foros.repository.ForoRepository;
import com.template.auth.foros.repository.ForoVotoRepository;
import com.template.auth.integration.moderation.dto.ModerationResponse;
import com.template.auth.integration.moderation.exception.ModerationApiException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForoService {
    private final UsuarioRepository usuarios;
    private final ForoRepository publicaciones;
    private final ComentarioForoRepository respuestas;
    private final ForoVotoRepository votos;
    private final ComentarioVotoRepository votosRespuesta;

    private final ModeracionService moderacionService;
    private final ForoModeracionEstadoService foroModeracionEstadoService;

    private final NotificacionService notificacionService;

    private final ModeracionWorkflowService moderacionWorkflowService;

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Foro publicacion(Long id) {
        return publicaciones.findById(id)
                .filter(foro -> "PUBLICADO".equals(foro.getEstadoPublicacion()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"
                ));
    }

    private ComentarioForo respuestaVisible(Long id) {
        return respuestas.findByComentarioIdAndEstadoPublicacionAndEstadoModeracion(
                        id,
                        "PUBLICADO",
                        "APROBADO"
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Respuesta no encontrada"
                ));
    }

    private Map<String, Object> salidaPublicacion(Foro p, Long usuarioId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getForoId());
        m.put("categoria", p.getCategoria() == null ? null : p.getCategoria().name());
        m.put("titulo", p.getTitulo());
        m.put("contenido", p.getDescripcion());
        m.put("autor", p.getUsuario().getNombre() + " " + p.getUsuario().getApellido());
        m.put("autorId", p.getUsuario().getId());
        m.put("creadoEn", p.getFechaCreacion());
        m.put("respuestas", respuestas.countByForo_ForoIdAndEstadoPublicacionAndEstadoModeracion(
                p.getForoId(), "PUBLICADO", "APROBADO"));
        m.put("puntos", votos.sumarValorPorForoId(p.getForoId()));
        m.put("miVoto", votos.findByForo_ForoIdAndUsuario_Id(p.getForoId(), usuarioId).map(ForoVoto::getValor).orElse(0));
        return m;
    }

    private Map<String, Object> salidaRespuesta(ComentarioForo r, Long usuarioId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getComentarioId());
        m.put("publicacionId", r.getForo().getForoId());
        m.put("contenido", r.getContenidoTexto());
        m.put("autor", r.getUsuario().getNombre() + " " + r.getUsuario().getApellido());
        m.put("autorId", r.getUsuario().getId());
        m.put("creadoEn", r.getFechaCreacion());
        m.put("puntos", votosRespuesta.sumarValorPorComentarioId(r.getComentarioId()));
        m.put("miVoto", votosRespuesta.findByComentario_ComentarioIdAndUsuario_Id(r.getComentarioId(), usuarioId).map(ComentarioVoto::getValor).orElse(0));
        return m;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listar(String email, ForoCategoria categoria, String query, int pagina) {
        Long userId = usuario(email).getId();
        String texto = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return publicaciones.buscarPorCategoriaYTexto(categoria, texto, PageRequest.of(Math.max(0, pagina), 10, Sort.by(Sort.Direction.DESC, "fechaCreacion")))
                .map(p -> salidaPublicacion(p, userId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detalle(String email, Long id) {
        Long userId = usuario(email).getId();
        Foro p = publicacion(id);
        Map<String, Object> resultado = new LinkedHashMap<>(salidaPublicacion(p, userId));
        resultado.put("listaRespuestas", respuestas
                .findByForo_ForoIdAndEstadoPublicacionAndEstadoModeracionOrderByFechaCreacionAsc(
                        id, "PUBLICADO", "APROBADO")
                .stream()
                .map(r -> salidaRespuesta(r, userId))
                .toList());
        return resultado;
    }

    public Map<String, Object> crear(String email, ForoCrearRequest req) {

        Usuario autor = usuario(email);

        // 1. Construir la publicación.
        Foro foro = new Foro();

        foro.setUsuario(autor);
        foro.setCategoria(req.categoria());
        foro.setTitulo(req.titulo().trim());
        foro.setDescripcion(req.contenido().trim());

        // 2. Establecer los estados iniciales.
        foro.setEstadoPublicacion("BORRADOR");
        foro.setEstadoModeracion("PENDIENTE");

        // 3. Guardar antes de llamar a Python.
        Foro guardado = publicaciones.saveAndFlush(foro);

        // 4. Construir el texto que recibirá el modelo.
        String texto = guardado.getTitulo()
                + " [SEP] "
                + guardado.getDescripcion();

        // 5. Solicitar predicción a Python.
        ModerationResponse prediccion =
                moderacionService.predecir(texto);

        // 6. Persistir moderación, actualizar estados y notificar.
        Moderacion moderacion =
                moderacionWorkflowService.procesarForo(
                        guardado.getForoId(),
                        prediccion
                );

        DecisionModeracion decision = moderacion.getDecision();

        // 6. Actualizar los estados de la publicación.
        foroModeracionEstadoService.actualizarEstado(
                guardado.getForoId(),
                decision
        );

        // 6.1. Notificar a los moderadores si requiere revisión humana.
        notificacionService.notificarRevision(moderacion);

        // 7. Reflejar los estados definitivos en el objeto.
        switch (decision) {

            case APROBADO -> {
                guardado.setEstadoPublicacion("PUBLICADO");
                guardado.setEstadoModeracion("APROBADO");
            }

            case RECHAZADO -> {
                guardado.setEstadoPublicacion("OCULTO");
                guardado.setEstadoModeracion("RECHAZADO");
            }

            case REQUIERE_REVISION -> {
                guardado.setEstadoPublicacion("OCULTO");
                guardado.setEstadoModeracion("REQUIERE_REVISION");
            }

            case PENDIENTE -> {
                guardado.setEstadoPublicacion("BORRADOR");
                guardado.setEstadoModeracion("PENDIENTE");
            }
        }

        // 8. Devolver la publicación.
        return salidaPublicacion(guardado, autor.getId());
    }

    public Map<String, Object> responder(String email, Long id, RespuestaCrearRequest req) {
        Foro p = publicacion(id);
        Usuario autor = usuario(email);
        ComentarioForo r = new ComentarioForo();
        r.setForo(p);
        r.setUsuario(autor);
        r.setContenidoTexto(req.contenido().trim());
        r.setEstadoPublicacion("BORRADOR");
        r.setEstadoModeracion("PENDIENTE");

        ComentarioForo guardado = respuestas.saveAndFlush(r);

        ModerationResponse prediccion;

        try {
            prediccion = moderacionService.predecir(
                    guardado.getContenidoTexto()
            );

        } catch (ModerationApiException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible completar la moderación del comentario. Inténtalo de nuevo más tarde.",
                    ex
            );
        }

        Moderacion moderacion =
                moderacionWorkflowService.procesarComentario(
                        guardado.getComentarioId(),
                        prediccion
                );

        if (moderacion == null || moderacion.getDecision() == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La moderación no devolvió una decisión válida."
            );
        }

        return salidaRespuesta(guardado, autor.getId());
    }

    @Transactional
    public Map<String, Object> votar(String email, Long id, int valor) {
        Usuario u = usuario(email);
        Foro p = publicacion(id);
        if (p.getUsuario().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes votar tu propia publicación.");
        ForoVoto v = votos.findByForo_ForoIdAndUsuario_Id(id, u.getId()).orElse(null);
        if (valor == 0) {
            if (v != null) votos.delete(v);
        } else {
            if (v == null) {
                v = new ForoVoto();
                v.setForo(p);
                v.setUsuario(u);
            }
            v.setValor(valor);
            votos.save(v);
        }
        votos.flush();
        return Map.of("puntos", votos.sumarValorPorForoId(id), "miVoto", valor);
    }

    @Transactional
    public Map<String, Object> votarRespuesta(String email, Long publicacionId, Long respuestaId, int valor) {
        Usuario u = usuario(email);
        publicacion(publicacionId);
        ComentarioForo r = respuestaVisible(respuestaId);
        if (!r.getForo().getForoId().equals(publicacionId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (r.getUsuario().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes votar tu propia respuesta.");
        ComentarioVoto v = votosRespuesta.findByComentario_ComentarioIdAndUsuario_Id(respuestaId, u.getId()).orElse(null);
        if (valor == 0) {
            if (v != null) votosRespuesta.delete(v);
        } else {
            if (v == null) {
                v = new ComentarioVoto();
                v.setComentario(r);
                v.setUsuario(u);
            }
            v.setValor(valor);
            votosRespuesta.save(v);
        }
        votosRespuesta.flush();
        return Map.of("puntos", votosRespuesta.sumarValorPorComentarioId(respuestaId), "miVoto", valor);
    }
}
