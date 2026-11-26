package com.template.auth.auth.exception;

public class RefreshTokenInvalidoException extends RuntimeException {

    public RefreshTokenInvalidoException() {
        super("Refresh token inválido");
    }
}
