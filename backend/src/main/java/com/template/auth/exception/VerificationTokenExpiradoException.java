package com.template.auth.exception;

public class VerificationTokenExpiradoException extends RuntimeException {

    public VerificationTokenExpiradoException() {
        super("Token de verificación expirado");
    }
}
