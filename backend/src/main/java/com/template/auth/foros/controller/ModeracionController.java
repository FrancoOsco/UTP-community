package com.template.auth.foros.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.template.auth.foros.dto.ModeracionAsignacionResponse;
import com.template.auth.foros.dto.ModeracionBandejaResponse;
import com.template.auth.foros.dto.ModeracionDecisionRequest;
import com.template.auth.foros.dto.ModeracionResolucionResponse;
import com.template.auth.foros.service.ModeracionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/moderacion")
@RequiredArgsConstructor
public class ModeracionController {

    private final ModeracionService moderacionService;

    @GetMapping("/bandeja")
    public List<ModeracionBandejaResponse> obtenerBandeja(Authentication auth) {
        return moderacionService.obtenerBandeja(auth.getName());
    }

    @GetMapping("/mis-revisiones")
    public List<ModeracionBandejaResponse> obtenerMisRevisiones(Authentication auth) {
        return moderacionService.obtenerMisRevisiones(auth.getName());
    }

    @PostMapping("/revisiones/{moderacionId}/tomar")
    public ModeracionAsignacionResponse tomarRevision(
            Authentication auth,
            @PathVariable Long moderacionId
    ) {
        return moderacionService.tomarRevision(
                auth.getName(),
                moderacionId
        );
    }

    @DeleteMapping("/revisiones/{moderacionId}/reserva")
    public ResponseEntity<Void> liberarRevision(
            Authentication auth,
            @PathVariable Long moderacionId
    ) {
        moderacionService.liberarRevision(auth.getName(), moderacionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/revisiones/{moderacionId}/decision")
    public ModeracionResolucionResponse resolverRevision(
            Authentication auth,
            @PathVariable Long moderacionId,
            @Valid @RequestBody ModeracionDecisionRequest request
    ) {
        return moderacionService.resolverRevision(
                auth.getName(),
                moderacionId,
                request.decision(),
                request.motivo()
        );
    }
}
