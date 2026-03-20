package buzmakov.Prototype;


import buzmakov.Prototype.rules.AnalysisRule;
import buzmakov.Prototype.service.LlmService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextAnalyzer {

    @NonNull
    final List<AnalysisRule> analysisRules;

    @NonNull
    final LlmService llmService;

    final Map<String, Recommendation> recommendationLibrary = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {
        loadRecommendations();
    }

    private void loadRecommendations() throws Exception {
        try (val is = getClass().getResourceAsStream("/recommendations.json")) {
            if (is == null) {
                log.warn(">>> Файл recommendations.json не найден. Будут использованы базовые советы.");
                return;
            }
            val mapper = new ObjectMapper();
            val loaded = mapper.readValue(is, new TypeReference<Map<String, Recommendation>>() {});
            recommendationLibrary.putAll(loaded);
            log.info(">>> Справочник рекомендаций загружен: {} категорий", recommendationLibrary.size());
        } catch (Exception thrown) {
            log.error("Ошибка при чтении recommendations.json: {}", thrown.getMessage());
            throw thrown;
        }
    }

    public AnalysisResult analyzeRuleBased(final String input) {
        val result = new AnalysisResult();

        for (AnalysisRule rule : analysisRules) {
            try {
                rule.apply(input, result);
            } catch (Exception e) {
                log.error("Ошибка в правиле {}: {}", rule.getName(), e.getMessage());
            }
        }
        return result;
    }

    public String analyzeWithLlm(final String input) {
        if (!llmService.isAvailable()) {
            log.warn("LLM-сервис недоступен, возвращаем fallback-ответ");
            return buildFallbackResponse();
        }

        String rawLlmResponse = llmService.analyzeAggression(input);
        return parseLlmResponse(rawLlmResponse);
    }

    private String parseLlmResponse(String rawResponse) {
        try {
            // убираем лишние пробелы и пустые строки
            List<String> lines = rawResponse.trim().lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

            if (lines.isEmpty()) return formatFinalResponse("OT-99", 50);

            // ищем строку, похожую на ID категории (две буквы, дефис, две цифры)
            String categoryId = lines.stream()
                .filter(s -> s.matches(".*[A-Z]{2}-\\d{2}.*"))
                .findFirst()
                .map(s -> s.replaceAll(".*([A-Z]{2}-\\d{2}).*", "$1"))
                .orElse("OT-99");

            // ищем строку, содержащую только цифры (score)
            int score = lines.stream()
                .filter(s -> s.matches("\\d+"))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(50);

            return formatFinalResponse(categoryId, score);
        } catch (Exception e) {
            log.error("Ошибка парсинга: {}", rawResponse);
            return formatFinalResponse("OT-99", 50);
        }
    }

    private String formatFinalResponse(String categoryId, int score) {
        Recommendation rec = recommendationLibrary.getOrDefault(categoryId,
            recommendationLibrary.get("OT-99"));

        return """
                [АНАЛИЗ ИИ]
                Уровень агрессии: %d%%
                Категория: %s (%s)
                Рекомендация: %s
                """.formatted(score, categoryId, rec.title(), rec.advice());
    }

    private String buildFallbackResponse() {
        return """
                Уровень агрессии: 0%%;
                Краткая рекомендация: LLM-сервис временно недоступен.
                """;
    }
}