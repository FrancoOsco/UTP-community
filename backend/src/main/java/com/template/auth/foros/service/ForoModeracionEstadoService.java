package com.template.auth.foros.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.template.auth.foros.entity.Foro;
import com.template.auth.foros.enums.DecisionModeracion;
import com.template.auth.foros.repository.ForoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForoModeracionEstadoService {

    private final ForoRepository publicaciones;

    @Transactional
    public void actualizarEstado(Long foroId, DecisionModeracion decision) {

        Foro foro = publicaciones.findById(foroId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"
                ));

        switch (decision) {

            case APROBADO -> {
                foro.setEstadoPublicacion("PUBLICADO");
                foro.setEstadoModeracion("APROBADO");
            }

            case RECHAZADO -> {
                foro.setEstadoPublicacion("OCULTO");
                foro.setEstadoModeracion("RECHAZADO");
            }

            case REQUIERE_REVISION -> {
                foro.setEstadoPublicacion("OCULTO");
                foro.setEstadoModeracion("REQUIERE_REVISION");
            }

            case PENDIENTE -> {
                foro.setEstadoPublicacion("BORRADOR");
                foro.setEstadoModeracion("PENDIENTE");
            }
        }
    }
}