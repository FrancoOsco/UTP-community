package com.template.auth.auth.exception;

public class PasswordReutilizadoException extends RuntimeException {

    public PasswordReutilizadoException() {
        super("La nueva contraseña debe ser diferente a la contraseña actual");
    }
}