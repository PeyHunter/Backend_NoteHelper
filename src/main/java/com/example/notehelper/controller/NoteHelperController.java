package com.example.notehelper.controller;

import com.example.notehelper.dto.InputDto;
import com.example.notehelper.service.OpenAiService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class NoteHelperController {

    @Value("${app.api-key}")
    private String apiKey;

    private final OpenAiService openAiService;

    public NoteHelperController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }



    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String text = extractText(file);

        String summary = openAiService.analyseText(
                text,
                "You are a helpful assistant that summarises documents clearly and concisely in the same language as they were written. " +
                        "Write short paragraphs with clear titles and dont use bullet points."
        ).block();

        return Map.of(
                "text", text,
                "summary", summary,
                "fileName", file.getOriginalFilename(),
                "timestamp", LocalDateTime.now().toString()
        );
    }



    // ---------- GENERATE NOTES ----------
    @PostMapping("/generate-notes")
    public Map<String, Object> generateNotes(@RequestBody Map<String, String> body) {
        String text = body.get("text");

        String prompt =
                "You are an experienced university instructor who creates detailed study notes to help students prepare for oral exams. "
                        + "Analyze the text below and rewrite it into full, in-depth notes in the same language as the source (keep Danish if the text is Danish). "
                        + "Always format each topic heading with Markdown syntax starting with '### '. "
                        + "Write as if you are explaining the subject to a student in class — go deep, explain why and how things work, and connect the ideas together. "
                        + "For every topic or concept you find, follow this structure:\n"
                        + "1) Write a clear section heading using Markdown syntax, for example '### Topic Title'.\n"
                        + "2) Write several complete paragraphs (at least 5–10 sentences each) that cover:\n"
                        + "   - What the concept means and how it is used\n"
                        + "   - How it works technically\n"
                        + "   - Why it is important in practice\n"
                        + "   - Tools, commands, or configuration details involved (use Markdown inline code formatting, e.g. `ping`, `docker compose up`, `ls -la`).\n"
                        + "   - A short, realistic example illustrating how it works or how it would be demonstrated in an exam.\n\n"
                        + "3) End each topic with a short wrap‑up paragraph, such as 'In summary…' or 'Key takeaway:'.\n\n"
                        + "VERY IMPORTANT: every topic must be separated by a blank line, then a line of exactly three hyphens '---', then another blank line.\n\n"
                        + "Do not use bullet points, numbered lists, or dashes at the start of lines. "
                        + "Write only in fully connected paragraphs. "
                        + "Make sure to expand on **every** relevant topic from the text — never stop mid‑explanation. "
                        + "It’s better to write too much detail than too little. "
                        + "The result should be thorough enough that a student could pass an oral exam using only your notes.\n\n"
                        + "Here is the source material:\n\n"
                        + text
                        + "\n\nNow write the complete expanded notes in Markdown format with '###' as section titles and '---' as topic separators.";

        String result = openAiService.analyseText(prompt,
                "You are an experienced teacher who writes long, paragraph-based explanations; no bullet points.").block();

        return Map.of(
                "summary", result,
                "timestamp", LocalDateTime.now().toString(),
                "success", true
        );
    }



    @PostMapping("/analyse-with-file")
    public Map<String, Object> analyseWithFile(@RequestBody Map<String, String> body) {
        String fileText = body.get("fileText");
        String question = body.get("question");

        String combinedPrompt =
                "Analyse the following document and answer the user's question based strictly on its content.\n\n"
                        + "DOCUMENT:\n" + fileText + "\n\n"
                        + "QUESTION:\n" + question;

        String result = openAiService.analyseText(
                combinedPrompt,
                "You are an academic tutor. Answer clearly, in the same language as the document, using complete sentences. "
                        + "Do not give bullet points or numbered lists."
        ).block();

        return Map.of(
                "summary", result,
                "timestamp", LocalDateTime.now().toString()
        );
    }

    // ---------- HELPER ----------
    private String extractText(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename().toLowerCase();

        if (name.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (name.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            throw new IOException("Unsupported file type. Please upload .txt or .pdf");
        }
    }





    // ---------- CHECK ----------
    @GetMapping("/check-key")
    public String checkKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            return "❌ API key not loaded";
        }
        return "✅ API key loaded: " + apiKey.substring(0, 7) + "...";
    }


}