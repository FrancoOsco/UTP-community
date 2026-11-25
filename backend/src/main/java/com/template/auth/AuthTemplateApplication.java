package com.template.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.template.auth.foros.entity.Moderacion;
import com.template.auth.foros.service.ModeracionService;
import com.template.auth.integration.moderation.ModerationApiClient;
import com.template.auth.integration.moderation.dto.ModerationHealthResponse;
import com.template.auth.integration.moderation.dto.ModerationResponse;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthTemplateApplication.class, args);
    }

    @Bean
    CommandLineRunner probarModerationHealth(ModerationApiClient moderationApiClient) {
        return args -> {
            ModerationHealthResponse health = moderationApiClient.health();
            System.out.println(health);
        };
    }

    @Bean
    CommandLineRunner probarModerationClient(
            ModerationApiClient moderationApiClient,
            @Value("${moderation.test-case:predict}") String testCase) {
        return args -> {
            String sampleText = "Ayuda con algoritmos [SEP] ¿Alguien puede explicarme este ejercicio?";
            System.out.println("=== Ejecutando caso de prueba: " + testCase + " ===");

            try {
                switch (testCase) {
                    case "predict" -> {
                        ModerationResponse response = moderationApiClient.predict(sampleText);
                        System.out.println("Etiqueta: " + response.label());
                        System.out.println("Score: " + response.score());
                        System.out.println("Modelo: " + response.modelName());
                    }
                    case "auth-error" -> {
                        moderationApiClient.predict(sampleText);
                        System.out.println("ADVERTENCIA: No se lanzó ModerationApiAuthenticationException");
                    }
                    case "unavailable" -> {
                        moderationApiClient.predict(sampleText);
                        System.out.println("ADVERTENCIA: No se lanzó ModerationApiUnavailableException");
                    }
                    case "timeout" -> {
                        ModerationResponse response = moderationApiClient.predict(sampleText);
                        System.out.println("Completó dentro de 1ms (label=" + response.label() + ")");
                    }
                    default -> System.out.println("Caso desconocido: " + testCase);
                }
            } catch (Exception ex) {
                System.out.println("Excepción capturada: " + ex.getClass().getName() + " -> " + ex.getMessage());
            }
        };
    }

    @Bean
    CommandLineRunner probarModeracionService(ModeracionService moderacionService) {
        return args -> {
            Long foroIdExistente = (long) 2;
            String texto = "Ayuda con algoritmos [SEP] ¿Alguien puede explicarme cómo resolver este ejercicio?";

            Moderacion moderacion = moderacionService.evaluarForo(foroIdExistente, texto);

            System.out.println("ID: " + moderacion.getModeracionId());
            System.out.println("Decisión: " + moderacion.getDecision());
            System.out.println("Score: " + moderacion.getModerationScore());
        };
    }
}