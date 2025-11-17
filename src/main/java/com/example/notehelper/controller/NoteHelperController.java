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
                // --- 🔧 PART 1: MARKDOWN + STRUCTURE RULES ---
                "Follow these Markdown structure rules carefully:\n"
                        + "1. Use '### ' for every main heading (for example: ### Introduction to SPAs).\n"
                        + "2. Use long paragraphs after each heading — at least 10–15 sentences.\n"
                        + "3. Separate each topic section with three hyphens (`---`) on its own line.\n"
                        + "4. Use inline code (like `npm start`, `function App() {}`) for technical terms and commands.\n"
                        + "5. For any block of code, always use fenced code blocks (```js, ```java, ```html, etc.) "
                        + "and ensure your explanations mention them as examples written in monospace font.\n"
                        + "6. Do not use bullet points unless absolutely required for clarity.\n"
                        + "7. Keep everything in the same language as the input text.\n\n"

                        // --- 🎓 PART 2: TEACHING STYLE RULES ---
                        + "Write as if you are a friendly university lecturer teaching a student who is *completely new* to the subject.\n"
                        + "You must make every idea understandable — walk the student through how things work and how to start using them.\n"
                        + "Your tone should be warm, clear, detailed, and patient. Sound like a human teacher, not a textbook.\n\n"
                        + "Each section **must include all of the following elements:**\n"
                        + "- A plain-language explanation of the concept (avoid jargon).\n"
                        + "- Why this concept matters or where it’s used in the real world.\n"
                        + "- A \"How to get started\" paragraph with concrete steps or setup examples.\n"
                        + "- At least one real example, preferably containing short code snippets or commands.\n"
                        + "- An extra tip or common mistake beginners should know.\n"
                        + "- Optional: Add a short analogy if it helps the explanation (e.g., comparing client-server interaction to restaurant ordering).\n\n"

                        // --- 💬 PART 3: PURPOSE ---
                        + "These notes will be used for exam preparation by a student who struggles with theory. "
                        + "Make the notes educational and comprehensive enough that someone could understand the entire topic just by reading them.\n\n"

                        // --- 📘 PART 4: EXAMPLE OUTPUT FORMAT ---
                        + "Example format:\n\n"
                        + "### Example Topic Title\n"
                        + "Explain what the concept is, what problem it solves, and how it works internally. "
                        + "Then, teach how to start using it step-by-step.\n\n"
                        + "```js\n"
                        + "// Example code snippet\n"
                        + "const app = document.getElementById('root');\n"
                        + "```\n\n"
                        + "After the example, describe what happens in the code and what the output means in plain language.\n\n"
                        + "---\n\n"
                        + "### Next Topic Title\n"
                        + "Continue with the same format.\n\n"

                        // --- 📄 PART 5: INPUT CONTENT ---
                        + "Now rewrite the following text into complete, beginner-friendly, detailed Markdown notes following all the rules above.\n\n"
                        + text;

        String result = openAiService.analyseText(
                prompt,
                "You are an expert programming lecturer. You explain from the ground up, giving examples, code, and setup instructions. "
                        + "You never assume prior knowledge. Always include a 'How to get started' segment and at least one real world example for each topic. "
                        + "Write clearly, in full paragraphs, with natural teaching flow and Markdown formatting."
        ).block();

        return Map.of(
                "summary", result,
                "timestamp", LocalDateTime.now().toString(),
                "success", true
        );
    }

    @PostMapping("/explain-more")
    public Map<String, Object> explainMore(@RequestBody Map<String, String> body) {
        String text = body.get("text");

        String prompt =
                "Take the following generated notes and expand on them in much greater depth. "
                        + "Explain every concept more clearly and technically. "
                        + "Add real-world examples, step-by-step walkthroughs, small code demonstrations, and analogies. "
                        + "Do not summarize again; keep the headings but enrich each section. "
                        + "Keep the output formatted in Markdown.\n\n"
                        + text;

        String deeperNotes = openAiService.analyseText(
                prompt,
                "You are a senior lecturer explaining advanced details to a curious student. "
                        + "Provide in-depth but understandable content, with examples and occasional diagrams described in words. "
                        + "Use Markdown formatting throughout."
        ).block();

        return Map.of(
                "summary", deeperNotes,
                "timestamp", LocalDateTime.now().toString(),
                "success", true
        );
    }

    @PostMapping("/generate-quiz")
    public Map<String, Object> generateQuiz(@RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        Integer amount = body.containsKey("amount") ? (Integer) body.get("amount") : 10;
        String prompt =
                "You are a helpful university lecturer creating a simple quiz for students based on their study notes. "
                        + "Generate exactly " + amount + " questions and their direct, detailed answers based on the following text. "
                        + "Each answer should be a concise paragraph (around 5 lines) that directly explains the concept for the question. "
                        + "Do NOT use bullet points or lists in the answers. "
                        + "Keep everything in the same language as the input text"
                        + "All output, including questions and answers, MUST be in the same language as the input text"
                        + "Format each question clearly with a 'Question X:' prefix for the question. "
                        + "Below each question, provide the answer, prefixed with 'Korrekt svar: ' and the 5-line explanation. "
                        + "Ensure there's an empty line between the end of an answer and the start of the next question.\n\n"
                        + "Example format for one question-answer pair:\n"
                        + "Question 1: What is hash-pinning in GitHub Actions?\n"
                        + "Korrekt svar: Hash-pinning in GitHub Actions refers to referencing actions by their full Git commit SHA rather than by a branch name or tag. This practice enhances security by ensuring that the exact version of the action used is immutable and cannot be altered by a malicious actor pushing new code to a branch or updating a tag. It protects against supply chain attacks where a compromised action could inject malicious code into your workflow. By pinning to a specific SHA, you guarantee deterministic and consistent behavior of your workflows.\n\n"
                        + "Question 2: Explain the concept of 'hoisting' in JavaScript.\n"
                        + "Korrekt svar: Hoisting is a JavaScript mechanism where variable and function declarations are moved to the top of their containing scope during the compilation phase, before code execution. This means you can use a variable or call a function before it has been declared in the code. For 'var' variables, only the declaration is hoisted, not the initialization, so accessing it before assignment results in 'undefined'. Function declarations are fully hoisted, allowing them to be called anywhere within their scope.\n\n"
                        + "TEXT:\n" + text;

        // Denne del af koden returnerer stadig den rå Markdown-streng
        String quizMarkdown = openAiService.analyseText(
                prompt,
                "You are a clear and concise teacher generating questions and direct answers for a quiz. "
                        + "Adhere strictly to the requested 'Question X:' for questions and 'Korrekt svar: ' for answers, followed by plain text. "
                        + "Do NOT use Markdown headings, lists, or any other formatting that deviates from plain text for answers. "
                        + "Ensure there's an empty line between each answer and the subsequent question. Do not add any extra text or Markdown outside the generated quiz."
        ).block();

        return Map.of(
                "quizMarkdown", quizMarkdown,
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
        if (apiKey == null || apiKey.isEmpty())
        {
            return "❌ API key not loaded";
        }
        return "✅ API key loaded: " + apiKey.substring(0, 7) + "...";
    }


}