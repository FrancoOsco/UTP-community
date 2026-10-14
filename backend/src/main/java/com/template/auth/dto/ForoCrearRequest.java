package com.template.auth.dto;

import com.template.auth.entity.ForoCategoria;
import jakarta.validation.constraints.*;

public record ForoCrearRequest(@NotNull ForoCategoria categoria,
                               @NotBlank @Size(min = 8, max = 150) String titulo,
                               @NotBlank @Size(min = 15, max = 5000) String contenido) {
}
