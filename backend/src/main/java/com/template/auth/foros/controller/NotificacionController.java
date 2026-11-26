package com.template.auth.foros.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.template.auth.foros.dto.NotificacionResponse;
import com.template.auth.foros.service.NotificacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERADOR', 'ADMIN')")
public class NotificacionController {

    private final NotificacionService notificacionService;

    // Listar todas las notificaciones del usuario autenticado.
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificacionService.listar(authentication.getName())
        );
    }

    // Listar únicamente las notificaciones no leídas.
    @GetMapping("/no-leidas")
    public ResponseEntity<List<NotificacionResponse>> listarNoLeidas(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificacionService.listarNoLeidas(authentication.getName())
        );
    }

    // Obtener la cantidad de notificaciones no leídas.
    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                notificacionService.contarNoLeidas(authentication.getName())
        );
    }

    // Marcar una notificación como leída.
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeida(
            Authentication authentication,
            @PathVariable Long id
    ) {
        notificacionService.marcarComoLeida(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }
}