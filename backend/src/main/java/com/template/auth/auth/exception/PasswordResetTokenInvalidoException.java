package com.template.auth.auth.exception;

public class PasswordResetTokenInvalidoException extends RuntimeException {

    public PasswordResetTokenInvalidoException() {
        super("Token de recuperación inválido");
    }
}
