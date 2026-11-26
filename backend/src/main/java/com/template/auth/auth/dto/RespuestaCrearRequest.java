package com.template.auth.auth.dto;

import jakarta.validation.constraints.*;

public record RespuestaCrearRequest(@NotBlank @Size(min = 2, max = 3000) String contenido) {
}
