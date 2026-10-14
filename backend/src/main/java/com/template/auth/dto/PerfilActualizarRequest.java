package com.template.auth.dto;

import jakarta.validation.constraints.*;

public record PerfilActualizarRequest(
        @NotBlank @Size(max = 90) String nombre,
        @NotBlank @Size(max = 90) String apellido,
        @Size(max = 100) String carrera,
        @Min(1) @Max(14) Integer ciclo,
        @Size(max = 500) String biografia
) {
}
