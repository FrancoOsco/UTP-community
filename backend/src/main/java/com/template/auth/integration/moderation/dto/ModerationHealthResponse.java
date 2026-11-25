package com.template.auth.integration.moderation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModerationHealthResponse(
        String status,

        @JsonProperty("model_loaded")
        Boolean modelLoaded,

        @JsonProperty("model_version")
        String modelVersion
) {
}
