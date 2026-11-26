package com.template.auth.foros.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.template.auth.auth.entity.Usuario;
import com.template.auth.auth.repository.UsuarioRepository;
import com.template.auth.foros.dto.NotificacionResponse;
import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.entity.Notificacion;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String email) {
        Usuario usuario = obtenerUsuario(email);
        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarNoLeidas(String email) {
        Usuario usuario = obtenerUsuario(email);
        return notificacionRepository
                .findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String email) {
        Usuario usuario = obtenerUsuario(email);
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId());
    }

    /** Marca una notificación propia como leída; repetir la petición no produce error. */
    @Transactional
    public void marcarComoLeida(String email, Long notificacionId) {
        Usuario usuario = obtenerUsuario(email);
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .filter(n -> n.getUsuarioId().equals(usuario.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notificación no encontrada."
                ));

        if (!notificacion.isLeida()) {
            // La actualización atómica evita sobrescribir fecha_lectura y
            // mantiene seguro el caso de dos peticiones concurrentes.
            notificacionRepository.marcarComoLeida(notificacionId, usuario.getId());
        }
    }

    public void notificarRevision(Moderacion moderacion) {

        if (moderacion == null
                || moderacion.getDecision() != DecisionModeracion.REQUIERE_REVISION) {
            return;
        }

        notificacionRepository.generarParaModeracion(
                moderacion.getModeracionId()
        );
    }

    private Usuario obtenerUsuario(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Se requiere un usuario autenticado."
            );
        }

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "El usuario autenticado no existe."
                ));
    }

    private NotificacionResponse aRespuesta(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getNotificacionId(),
                notificacion.getModeracionId(),
                notificacion.getTipoContenido().name(),
                notificacion.getContenidoId(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getFechaLectura()
        );
    }
}
