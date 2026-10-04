package com.template.auth.controller;

import com.template.auth.dto.ForgotPasswordRequest;
import com.template.auth.dto.ResetPasswordRequest;
import com.template.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.solicitarRecuperacion(request.getEmail());
        return ResponseEntity.ok(
                Map.of("mensaje", "Si existe una cuenta asociada al correo, se enviaron instrucciones.")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.restablecerPassword(
                request.getToken(),
                request.getNuevaPassword()
        );

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }
}
