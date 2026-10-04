package com.template.auth.exception;

public class VerificationTokenInvalidoException extends RuntimeException {

    public VerificationTokenInvalidoException() {
        super("Token de verificación inválido");
    }
}
