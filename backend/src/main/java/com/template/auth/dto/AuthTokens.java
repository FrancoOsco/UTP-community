package com.template.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthTokens {

    private final String accessToken;
    private final String refreshToken;
}
