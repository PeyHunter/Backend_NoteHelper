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

    //
    public OpenAiService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }


    //Modtager min prompt + hvilken rolle den skal påtage sig
    public Mono<String> analyseText(String prompt, String systemRole) {

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemRole),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 15000,
                "temperature", 0.3
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

    public Mono<String> analyseMultipleSections(String text, String systemRole) {

        // Her opdeler vi prompten i tre dele, så OpenAI kan svare hurtigere parallelt
        String introPrompt =
                "Lav et kort introduktionsafsnit ud fra denne tekst:\n" + text;

        String examplesPrompt =
                "Lav nogle illustrative eller tekniske eksempler ud fra denne tekst:\n" + text;

        String howToPrompt =
                "Forklar hvordan man kommer i gang eller kan bruge emnet i praksis:\n" + text;

        // Mono.zip samler tre asynkrone kald, så de kører samtidig
        return Mono.zip(
                analyseText(introPrompt, systemRole),
                analyseText(examplesPrompt, systemRole),
                analyseText(howToPrompt, systemRole)
        ).map(tuple -> {
            String intro = tuple.getT1();
            String examples = tuple.getT2();
            String howTo = tuple.getT3();

            // Vi samler de tre resultater som én samlet markdown-tekst
            return "# Introduktion\n" + intro + "\n\n" +
                    "## Eksempler\n" + examples + "\n\n" +
                    "## Kom i gang\n" + howTo;
        });
    }

}
