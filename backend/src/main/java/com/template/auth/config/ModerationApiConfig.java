package com.template.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class ModerationApiConfig {

    @Bean
    public RestClient moderationRestClient(
            RestClient.Builder builder,

            @Value("${moderation.api.base-url}")
            String baseUrl,

            @Value("${moderation.api.connect-timeout}")
            Duration connectTimeout,

            @Value("${moderation.api.read-timeout}")
            Duration readTimeout
    ) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}