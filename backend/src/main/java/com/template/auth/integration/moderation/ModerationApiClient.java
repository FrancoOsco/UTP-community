package com.template.auth.integration.moderation;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.template.auth.integration.moderation.dto.ModerationHealthResponse;
import com.template.auth.integration.moderation.dto.ModerationRequest;
import com.template.auth.integration.moderation.dto.ModerationResponse;
import com.template.auth.integration.moderation.exception.ModerationApiAuthenticationException;
import com.template.auth.integration.moderation.exception.ModerationApiException;
import com.template.auth.integration.moderation.exception.ModerationApiRequestException;
import com.template.auth.integration.moderation.exception.ModerationApiTimeoutException;
import com.template.auth.integration.moderation.exception.ModerationApiUnavailableException;

@Service
public class ModerationApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public ModerationApiClient(
            RestClient moderationRestClient,
            @Value("${moderation.api.api-key}") String apiKey
    ) {
        this.restClient = moderationRestClient;
        this.apiKey = apiKey;
    }

    // Consultar el estado de FastAPI
    public ModerationHealthResponse health() {

        ModerationHealthResponse response = execute(
                "health",
                () -> restClient
                        .get()
                        .uri("/health")
                        .retrieve()
                        .body(ModerationHealthResponse.class)
        );

        if (response == null
                || !"ok".equalsIgnoreCase(response.status())
                || !Boolean.TRUE.equals(response.modelLoaded())) {

            throw new ModerationApiUnavailableException(
                    "La API de moderación no tiene el modelo disponible."
            );
        }

        return response;
    }

    // Enviar un texto para su clasificación
    public ModerationResponse predict(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "El texto que se enviará a moderación no puede estar vacío."
            );
        }

        ModerationRequest request = new ModerationRequest(texto);

        ModerationResponse response = execute(
                "predict",
                () -> restClient
                        .post()
                        .uri("/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", apiKey)
                        .body(request)
                        .retrieve()
                        .body(ModerationResponse.class)
        );

        if (response == null
                || response.label() == null
                || response.probability() == null
                || response.score() == null
                || response.modelVersion() == null
                || response.modelName() == null) {

            throw new ModerationApiException(
                    "La API de moderación devolvió una respuesta incompleta."
            );
        }

        return response;
    }

    // Ejecutar una petición y centralizar el manejo de errores
    private <T> T execute(
            String operation,
            Supplier<T> request
    ) {

        try {

            return request.get();

        } catch (RestClientResponseException ex) {

            int status = ex.getStatusCode().value();

            if (status == 401) {
                throw new ModerationApiAuthenticationException(
                        "La API de moderación rechazó la autenticación."
                );
            }

            if (status == 422) {
                throw new ModerationApiRequestException(
                        "La API de moderación rechazó los datos enviados."
                );
            }

            if (status >= 500) {
                throw new ModerationApiUnavailableException(
                        "La API de moderación presentó un error interno. HTTP "
                                + status,
                        ex
                );
            }

            throw new ModerationApiException(
                    "Error HTTP al ejecutar " + operation + ". Código: "
                            + status,
                    ex
            );

        } catch (ResourceAccessException ex) {

            if (isTimeout(ex)) {
                throw new ModerationApiTimeoutException(
                        "Se agotó el tiempo de espera de la API de moderación.",
                        ex
                );
            }

            throw new ModerationApiUnavailableException(
                    "No fue posible establecer comunicación con la API de moderación.",
                    ex
            );
        }
    }

    // Identificar errores de tiempo de espera
    private boolean isTimeout(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}