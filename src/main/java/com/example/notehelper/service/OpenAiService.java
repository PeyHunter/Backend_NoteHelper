package com.example.notehelper.service;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final WebClient webClient;

    @Value("${app.api-key}")
    private String apiKey;

    @Value("${app.url}")
    private String apiUrl;

    @Value("${app.model}")
    private String model;

    public OpenAiService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<String> analyseText(String text) {

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "Du er en hjælpsom tutor, der laver korte noter og nøglebegreber ud fra en given tekst."),
                        Map.of("role", "user",
                                "content", text)
                ),
                "max_tokens", 300,
                "temperature", 0.8
        );

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<Map<String, Object>> choices =
                                (List<Map<String, Object>>) response.get("choices");
                        Map<String, Object> message =
                                (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    } catch (Exception e) {
                        return "Fejl i svar: " + e.getMessage();
                    }
                });
    }

}
