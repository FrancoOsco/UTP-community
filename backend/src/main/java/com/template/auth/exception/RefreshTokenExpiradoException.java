package com.template.auth.exception;

public class RefreshTokenExpiradoException extends RuntimeException {

    public RefreshTokenExpiradoException() {
        super("Refresh token expirado");
    }
}
