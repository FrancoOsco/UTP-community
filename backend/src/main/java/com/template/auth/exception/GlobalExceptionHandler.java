package com.template.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException exception) {

        String mensaje = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Datos inválidos");

        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(
            CredencialesInvalidasException exception) {
        return crearRespuesta(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioNoEncontrado(
            UsuarioNoEncontradoException exception) {
        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> manejarEmailYaRegistrado(
            EmailYaRegistradoException exception) {
        return crearRespuesta(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(EmailNoVerificadoException.class)
    public ResponseEntity<Map<String, Object>> manejarEmailNoVerificado(
            EmailNoVerificadoException exception) {
        return crearRespuesta(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            RefreshTokenInvalidoException.class,
            RefreshTokenExpiradoException.class
    })
    public ResponseEntity<Map<String, Object>> manejarRefreshToken(
            RuntimeException exception) {
        return crearRespuesta(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            VerificationTokenInvalidoException.class,
            VerificationTokenExpiradoException.class
    })
    public ResponseEntity<Map<String, Object>> manejarVerificationToken(
            RuntimeException exception) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler({
            PasswordResetTokenInvalidoException.class,
            PasswordResetTokenExpiradoException.class
    })
    public ResponseEntity<Map<String, Object>> manejarPasswordResetToken(
            RuntimeException exception) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(PasswordReutilizadoException.class)
    public ResponseEntity<Map<String, Object>> manejarPasswordReutilizado(
            PasswordReutilizadoException exception) {

        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>> crearRespuesta(
            HttpStatus status,
            String mensaje) {

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "status", status.value(),
                        "message", mensaje
                ));
    }
}