package com.template.auth.auth.exception;

public class RefreshTokenExpiradoException extends RuntimeException {

    public RefreshTokenExpiradoException() {
        super("Refresh token expirado");
    }
}
