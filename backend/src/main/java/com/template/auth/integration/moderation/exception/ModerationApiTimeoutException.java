package com.template.auth.integration.moderation.exception;

public class ModerationApiTimeoutException extends ModerationApiUnavailableException {

    public ModerationApiTimeoutException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}