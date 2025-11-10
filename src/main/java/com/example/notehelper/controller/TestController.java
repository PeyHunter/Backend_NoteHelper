package com.example.notehelper.controller;

import com.example.notehelper.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class TestController {

    @Value("${app.api-key}")
    private String apiKey;

    private final OpenAiService openAiService;

    public TestController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    // MAPPING
    @GetMapping("/check-key")
    public String checkKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            return "❌ API key not loaded";
        }
        // return kun de første tegn, så du ikke viser hele nøglen
        return "✅ API key loaded: " + apiKey.substring(0, 7) + "...";
    }


    @PostMapping("/analyse")
    public Mono<String> analyse(@RequestBody String text) {
        return openAiService.analyseText(text);
    }






}
