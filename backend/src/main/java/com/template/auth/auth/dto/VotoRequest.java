package com.template.auth.auth.dto;

import jakarta.validation.constraints.*;

public record VotoRequest(@NotNull @Min(-1) @Max(1) Integer valor) {
}
