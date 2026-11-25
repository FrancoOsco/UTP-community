package com.template.auth.foros.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.template.auth.foros.entity.ComentarioForo;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.repository.ComentarioForoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComentarioModeracionEstadoService {

    private final ComentarioForoRepository comentarios;

    @Transactional
    public void actualizarEstado(Long comentarioId, DecisionModeracion decision) {
        ComentarioForo comentario = comentarios.findById(comentarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comentario no encontrado"
                ));

        switch (decision) {
            case APROBADO -> {
                comentario.setEstadoPublicacion("PUBLICADO");
                comentario.setEstadoModeracion("APROBADO");
            }
            case RECHAZADO -> {
                comentario.setEstadoPublicacion("OCULTO");
                comentario.setEstadoModeracion("RECHAZADO");
            }
            case REQUIERE_REVISION -> {
                comentario.setEstadoPublicacion("OCULTO");
                comentario.setEstadoModeracion("REQUIERE_REVISION");
            }
            case PENDIENTE -> {
                comentario.setEstadoPublicacion("BORRADOR");
                comentario.setEstadoModeracion("PENDIENTE");
            }
        }
    }
}
