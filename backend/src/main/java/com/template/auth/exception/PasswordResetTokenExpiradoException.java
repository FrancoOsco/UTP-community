package com.template.auth.exception;

public class PasswordResetTokenExpiradoException extends RuntimeException {

    public PasswordResetTokenExpiradoException() {
        super("Token de recuperación expirado");
    }
}
