package com.template.auth.auth.exception;

public class PasswordResetTokenExpiradoException extends RuntimeException {

    public PasswordResetTokenExpiradoException() {
        super("Token de recuperación expirado");
    }
}
