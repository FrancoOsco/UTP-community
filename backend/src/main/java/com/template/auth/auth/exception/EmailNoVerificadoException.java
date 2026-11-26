package com.template.auth.auth.exception;

public class EmailNoVerificadoException extends RuntimeException {

    public EmailNoVerificadoException() {
        super("Debes verificar tu correo antes de iniciar sesión");
    }
}
