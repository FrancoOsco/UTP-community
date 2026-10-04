package com.template.auth.exception;

public class PasswordResetTokenInvalidoException extends RuntimeException {

    public PasswordResetTokenInvalidoException() {
        super("Token de recuperación inválido");
    }
}
