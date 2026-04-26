package buzmakov.Prototype;

import buzmakov.Prototype.model.AuthorRole;
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
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
@Slf4j
public class TextAnalyzer {

    @NonNull
    List<AnalysisRule> analysisRules;

    @NonNull
    LlmService llmService;

    @NonFinal
    Map<String, Recommendation> clientLibrary = new HashMap<>();

    @NonFinal
    Map<String, Recommendation> supportLibrary = new HashMap<>();

    FeedbackService feedbackService;

    ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        if (analysisRules.isEmpty()) {
            log.error("Критическая ошибка: Список правил (AnalysisRule) пуст. Убедитесь, что правила помечены @Component.");
        } else {
            log.info("Успешно инициализировано правил анализа: {}", analysisRules.size());
            analysisRules.forEach(rule -> log.info("Зарегистрировано правило: {}", rule.getName()));
        }

        loadLibrary("/client_recommendations.json", clientLibrary);
        loadLibrary("/support_recommendations.json", supportLibrary);
    }

    private void loadLibrary(final String path, final Map<String, Recommendation> targetMap) throws Exception {
        try (val inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                log.warn("Файл справочника {} не найден в ресурсах.", path);
                return;
            }

            val loaded = mapper.readValue(inputStream, new TypeReference<Map<String, Recommendation>>() {
            });
            targetMap.putAll(loaded);

            log.info("Справочник {} загружен. Категорий: {}", path, targetMap.size());
        } catch (final Exception thrown) {
            log.error("Ошибка при чтении справочника {}: {}", path, thrown.getMessage());
            throw thrown;
        }
    }

    public AnalysisResult analyzeRuleBased(@NonNull final String input, @NonNull final AuthorRole role) {
        val result = new AnalysisResult();

        analysisRules.forEach(rule -> {
            try {
                rule.apply(input, result);
            } catch (final Exception thrown) {
                log.error("Ошибка выполнения правила {}: {}", rule.getName(), thrown.getMessage());
            }
        });

        return result;
    }

    public record LlmResult(
        String categoryId,
        int score,
        String recommendation,
        String formattedResponse
    ) {
    }

    public LlmResult analyzeWithLlm(@NonNull final String input, @NonNull final AuthorRole role) {
        val manualMatch = feedbackService.findMatch(input);

        if (manualMatch.isPresent()) {
            val feedback = manualMatch.get();
            log.info("Применена ручная коррекция.");

            val targetLibrary = (role == AuthorRole.CLIENT) ? clientLibrary : supportLibrary;
            val recData = ofNullable(targetLibrary.get(feedback.category()))
                .orElseGet(() -> targetLibrary.get("OK-00"));

            return new LlmResult(
                feedback.category(),
                feedback.score(),
                ofNullable(recData).map(Recommendation::advice).orElse(""),
                formatFinalResponse(feedback.category(), feedback.score(), role));
        }

        if (!llmService.isAvailable()) {
            log.warn("LLM недоступен. Возврат заглушки.");
            return new LlmResult("OK-00", 0, "Сервис недоступен", buildFallbackResponse());
        }

        val rawLlmResponse = llmService.analyzeAggression(input, role);
        return parseLlmResponse(rawLlmResponse, role);
    }

    public AnalysisResult convertLlmToAnalysisResult(final LlmResult llmResult) {
        val result = new AnalysisResult();

        result.setCategory(llmResult.categoryId());
        result.setScore(llmResult.score());
        result.setRecommendation(llmResult.recommendation());

        result.getReport()
            .append("\n[СЛОЙ 2] Анализ нейросетью. Вердикт: ")
            .append(llmResult.categoryId());

        return result;
    }

    private LlmResult parseLlmResponse(final String rawResponse, final AuthorRole role) {
        try {
            val lines = rawResponse.trim()
                .lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();

            val targetLibrary = (role == AuthorRole.CLIENT) ? clientLibrary : supportLibrary;

            if (lines.isEmpty()) {
                val defaultRec = targetLibrary.get("OT-99");
                val advice = ofNullable(defaultRec).map(Recommendation::advice).orElse("");
                return new LlmResult("OT-99", 50, advice, formatFinalResponse("OT-99", 50, role));
            }

            val categoryId = lines.stream()
                .filter(line -> line.matches(".*[A-Z]{2}-\\d{2}.*"))
                .findFirst()
                .map(line -> line.replaceAll(".*([A-Z]{2}-\\d{2}).*", "$1"))
                .orElse("OT-99");

            val score = lines.stream()
                .filter(line -> line.matches("\\d+"))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(50);

            val recData = ofNullable(targetLibrary.get(categoryId))
                .orElseGet(() -> targetLibrary.get("OK-00"));

            val advice = ofNullable(recData).map(Recommendation::advice).orElse("");
            return new LlmResult(categoryId, score, advice, formatFinalResponse(categoryId, score, role));
        } catch (final Exception thrown) {
            log.error("Ошибка парсинга LLM: {}", rawResponse);
            return new LlmResult("OT-99", 50, "", formatFinalResponse("OT-99", 50, role));
        }
    }

    private String formatFinalResponse(final String categoryId, final int score, final AuthorRole role) {
        val targetLibrary = (role == AuthorRole.CLIENT) ? clientLibrary : supportLibrary;
        val recommendation = ofNullable(targetLibrary.get(categoryId))
            .orElseGet(() -> targetLibrary.get("OK-00"));

        return """
                [АНАЛИЗ РОЛИ: %s]
                Уровень агрессии: %d%%
                Вердикт: %s (%s)
                Инструкция: %s
                """.formatted(
            role.name(),
            score,
            categoryId,
            recommendation.title(),
            recommendation.advice());
    }

    private String buildFallbackResponse() {
        return """
                Уровень агрессии: 0%%
                Краткая рекомендация: Сервис LLM временно недоступен.
                """;
    }
}