package com.template.auth.integration.moderation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModerationResponse(

        Integer label,

        Double probability,

        Double score,

        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("model_name")
        String modelName

) {
}