package com.template.auth.integration.moderation.exception;

public class ModerationApiException extends RuntimeException {

    public ModerationApiException(String message) {
        super(message);
    }

    public ModerationApiException(String message, Throwable cause) {
        super(message, cause);
    }
}