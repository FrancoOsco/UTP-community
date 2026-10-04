package com.template.auth.controller;

import com.template.auth.dto.AuthTokens;
import com.template.auth.dto.LoginRequest;
import com.template.auth.dto.LoginResponse;
import com.template.auth.dto.RefreshTokenResponse;
import com.template.auth.dto.UsuarioRequest;
import com.template.auth.dto.VerifyEmailRequest;
import com.template.auth.exception.RefreshTokenInvalidoException;
import com.template.auth.service.AuthService;
import com.template.auth.service.EmailVerificationService;
import com.template.auth.service.RefreshTokenCookieService;
import com.template.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody UsuarioRequest request) {

        authService.register(request);
        return ResponseEntity.ok(
                Map.of("mensaje", "Usuario registrado. Revisa tu correo para verificar la cuenta.")
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        emailVerificationService.verificarEmail(request.getToken());
        return ResponseEntity.ok(Map.of("mensaje", "Correo verificado correctamente."));
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthTokens tokens = authService.login(request);
        refreshTokenCookieService.agregarRefreshToken(response, tokens.getRefreshToken());
        return new LoginResponse(tokens.getAccessToken());
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = refreshTokenCookieService
                .obtenerRefreshToken(request)
                .orElseThrow(RefreshTokenInvalidoException::new);

        AuthTokens tokens = authService.refresh(refreshToken);
        refreshTokenCookieService.agregarRefreshToken(response, tokens.getRefreshToken());
        return new RefreshTokenResponse(tokens.getAccessToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        refreshTokenCookieService
                .obtenerRefreshToken(request)
                .ifPresent(refreshTokenService::logout);

        refreshTokenCookieService.eliminarRefreshToken(response);
        return ResponseEntity.noContent().build();
    }
}
