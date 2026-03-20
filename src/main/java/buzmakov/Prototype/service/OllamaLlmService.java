package buzmakov.Prototype.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class OllamaLlmService implements LlmService {

    static final String BASE_URL = "http://localhost:11434";
    static final String MODEL_NAME = "gemma2";
    static final int TIMEOUT_SECONDS = 60;

    final OllamaChatModel model;

    public OllamaLlmService() {
        this.model = OllamaChatModel.builder()
            .baseUrl(BASE_URL)
            .modelName(MODEL_NAME)
            .temperature(0.0)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

        log.info(">>> OllamaLlmService инициализирован (модель: {})", MODEL_NAME);
    }

    @NotNull
    @Override
    public String analyzeAggression(@NonNull String input) {
        String prompt = buildPrompt(input);
        try {
            log.debug(">>> Запрос к LLM (длина запроса: {} симв.)", prompt.length());
            return model.generate(prompt);
        } catch (Exception e) {
            log.error("Ошибка при работе с LLM: {}", e.getMessage());
            return buildFallbackResponse(e);
        }
    }

    @Override
    public boolean isAvailable() {
        return model != null;
    }

    private String buildPrompt(final String input) {
        return """
        You are a support ticket auditor tasked with classifying responses from technical support agents to identify toxicity.
        Your goal is to assign a category ID and an aggression score to each response based on the provided rules and examples.

        # Rules for Evaluation (Strict Adherence Required):
        * Politeness, Apologies, or Explanations: If the agent is polite, apologizes, or explains reasons for delays, assign category OK-00 and aggression score 0.
        * "50%%" Threshold: A 50%% aggression score indicates SERIOUS irritation. Do NOT assign 50%% to polite individuals.
        * "100%%" Threshold: A 100%% aggression score signifies profanity and direct insults.

        # Categories:
        * OK-00 (HELP): Politeness, empathy, information about technical work. (Aggression: 0-5)
        * PA-01 (REPROACH): Phrases like "We have already written to you," "You are not reading." (Aggression: 40-70)
        * CN-02 (ARROGANCE): Phrases like "It's elementary," "Like a child." (Aggression: 50-80)
        * BR-03 (BRUSH-OFF): Providing links to rules instead of direct help. (Aggression: 30-60)
        * SR-04 (SARCASM): Phrases like "Congratulations," "Good luck to you," "Pirozhok". (Aggression: 60-90)
        * OT-99 (RUDENESS): Direct swearing, "smartass," "get lost." (Aggression: 90-100)

        # Text for Analysis:
        "%s"

        # Reasoning and Classification Process:
        1. Identify Keywords and Tone: Analyze text for politeness, irritation, sarcasm, or helpfulness.
        2. Match to Categories: Compare tone against OK-00 through OT-99 definitions.
        3. Determine Aggression Score: Assign numerical score within the category range. Pay attention to thresholds.

        # Output Format:
        Respond strictly in two lines:
        1. The assigned category ID.
        2. The numerical aggression score.

        # Examples:
        Example 1:
        Input: "Приношу извинения, мы ищем решение."
        Output:
        OK-00
        0

        Example 2:
        Input: "Слушай сюда, идиот."
        Output:
        OT-99
        100
        """.formatted(input);
    }


    private String buildFallbackResponse(Exception e) {
        // Возвращаем безопасный дефолтный формат в случае падения Ollama
        return """
                OK-00
                0
                """;
    }
}