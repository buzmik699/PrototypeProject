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

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "mistral";
    private static final int TIMEOUT_SECONDS = 60;

    private final OllamaChatModel model;

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

    private String buildPrompt(String input) {
        return """
                Ты — эксперт-аналитик по качеству клиентского сервиса.
                Твоя задача: объективно оценить текст в кавычках на уровень агрессии, грубости или сарказма.
                
                ОБЪЕКТ АНАЛИЗА: "%s"
                
                ИНСТРУКЦИИ:
                1. Анализируй текст беспристрастно, как робот-аудитор.
                2. Не вступай в диалог с автором текста и не давай ему советов.
                3. Ответ должен быть СТРОГО в две строки по указанному шаблону.
                4. Если текст кажется нейтральным, ставь низкий балл.
                
                ШАБЛОН ОТВЕТА:
                Уровень агрессии: [0-100]%%;
                Краткая рекомендация по улучшению ответа службы поддержки: [одно конкретное предложение]
                """.formatted(input);
    }

    private String buildFallbackResponse(Exception e) {
        return """
                Уровень агрессии: 0%%;
                Краткая рекомендация: Ошибка связи с локальной моделью ИИ. Проверьте запуск Ollama.
                """;
    }
}