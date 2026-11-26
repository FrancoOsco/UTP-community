package com.template.auth.auth.exception;

public class VerificationTokenExpiradoException extends RuntimeException {

    public VerificationTokenExpiradoException() {
        super("Token de verificación expirado");
    }
}
