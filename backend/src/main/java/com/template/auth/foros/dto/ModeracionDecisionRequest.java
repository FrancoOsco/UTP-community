package com.template.auth.foros.dto;

import com.template.auth.foros.enums.DecisionFinalModerador;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModeracionDecisionRequest(
        @NotNull DecisionFinalModerador decision,
        @Size(max = 2000) String motivo
) {
}
