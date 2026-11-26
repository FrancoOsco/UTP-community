package com.template.auth.auth.exception;

public class VerificationTokenInvalidoException extends RuntimeException {

    public VerificationTokenInvalidoException() {
        super("Token de verificación inválido");
    }
}
