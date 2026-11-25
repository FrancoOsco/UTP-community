package com.template.auth.integration.moderation.exception;

public class ModerationApiUnavailableException
        extends ModerationApiException {

    public ModerationApiUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    public ModerationApiUnavailableException(String message) {
        super(message);
    }
}