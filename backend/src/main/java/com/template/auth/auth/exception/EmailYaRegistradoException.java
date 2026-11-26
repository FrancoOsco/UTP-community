package com.template.auth.auth.exception;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("El correo ya está registrado");
    }
}
