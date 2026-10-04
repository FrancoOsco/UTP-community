package com.template.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UsuarioResponse {

    private final Long id;
    private final String nombre;
    private final String apellido;
    private final String email;
    private final String rol;
}
