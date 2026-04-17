package buzmakov.Prototype;

import buzmakov.Prototype.rules.AnalysisRule;
import buzmakov.Prototype.service.FeedbackService;
import buzmakov.Prototype.service.LlmService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class TextAnalyzer {

    @NonNull
    List<AnalysisRule> analysisRules;

    @NonNull
    LlmService llmService;

    Map<String, Recommendation> recommendationLibrary = new HashMap<>();

    FeedbackService feedbackService;

    @PostConstruct
    public void init() throws Exception {
        loadRecommendations();
    }

    private void loadRecommendations() throws Exception {
        try (val inputStream = getClass().getResourceAsStream("/recommendations.json")) {
            if (inputStream == null) {
                log.warn(">>> Файл recommendations.json не найден. Будут использованы базовые советы.");
                return;
            }

            val mapper = new ObjectMapper();
            val loaded = mapper.readValue(inputStream, new TypeReference<Map<String, Recommendation>>() {
            });

            recommendationLibrary.putAll(loaded);

            if (log.isInfoEnabled()) {
                log.info(">>> Справочник рекомендаций загружен: {} категорий", recommendationLibrary.size());
            }
        } catch (Exception thrown) {
            log.error("Ошибка при чтении recommendations.json: {}", thrown.getMessage());
            throw thrown;
        }
    }

    public AnalysisResult analyzeRuleBased(final String input) {
        val result = new AnalysisResult();

        analysisRules.forEach(rule -> {
            try {
                rule.apply(input, result);
            } catch (Exception thrown) {
                log.error("Ошибка в правиле {}: {}", rule.getName(), thrown.getMessage());
            }
        });

        return result;
    }

    public record LlmResult(
        String categoryId,
        int score,
        String formattedResponse
    ) {}

    public LlmResult analyzeWithLlm(final String input) {
        val manualMatch = feedbackService.findMatch(input);

        if (manualMatch.isPresent()) {
            val feedback = manualMatch.get();
            log.info(">>> Используется ручная коррекция");

            return new LlmResult(
                feedback.category(),
                feedback.score(),
                formatFinalResponse(feedback.category(), feedback.score())
            );
        }

        if (!llmService.isAvailable()) {
            log.warn("LLM-сервис недоступен, возвращаем fallback-ответ");
            return new LlmResult("OK-00", 0, buildFallbackResponse());
        }

        val rawLlmResponse = llmService.analyzeAggression(input);
        return parseLlmResponse(rawLlmResponse);
    }

    private LlmResult parseLlmResponse(final String rawResponse) {
        try {
            val lines = rawResponse.trim()
                .lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();

            if (lines.isEmpty()) {
                return new LlmResult("OT-99", 50, formatFinalResponse("OT-99", 50));
            }

            val categoryId = lines.stream()
                .filter(line -> line.matches(".*[A-Z]{2}-\\d{2}.*"))
                .findFirst()
                .map(line -> line.replaceAll(".*([A-Z]{2}-\\d{2}).*", "$1"))
                .orElse("OT-99");

            int score = lines.stream()
                .filter(line -> line.matches("\\d+"))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(50);

            return new LlmResult(categoryId, score, formatFinalResponse(categoryId, score));
        } catch (Exception thrown) {
            log.error("Ошибка парсинга ответа LLM: {}", rawResponse);
            return new LlmResult("OT-99", 50, formatFinalResponse("OT-99", 50));
        }
    }

    private String formatFinalResponse(final String categoryId, final int score) {
        val recommendation = recommendationLibrary.getOrDefault(
            categoryId,
            recommendationLibrary.get("OT-99")
        );

        return """
                [АНАЛИЗ ИИ]
                Уровень агрессии: %d%%
                Категория: %s (%s)
                Рекомендация: %s
                """.formatted(score, categoryId, recommendation.title(), recommendation.advice());
    }

    private String buildFallbackResponse() {
        return """
                Уровень агрессии: 0%;
                Краткая рекомендация: LLM-сервис временно недоступен.
                """;
    }
}