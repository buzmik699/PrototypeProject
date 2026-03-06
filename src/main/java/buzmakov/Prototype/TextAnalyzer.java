package buzmakov.Prototype;

import buzmakov.Prototype.rules.AnalysisRule;
import buzmakov.Prototype.service.LlmService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextAnalyzer {
    @NonNull
    final List<AnalysisRule> analysisRules;

    @NonNull
    final List<SupportCase> cases = new ArrayList<>();

    @NonNull
    final LlmService llmService;  // ← Внедряем через интерфейс

    OllamaChatModel model;

    @PostConstruct
    public void init() throws Exception {
        loadDataset();
    }

    private void loadDataset() throws Exception {
        try (val is = getClass().getResourceAsStream("/dataset.json")) {
            if (is == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл dataset.json не найден!");
                return;
            }

            val mapper = new ObjectMapper();
            val loadedCases = mapper.readValue(is, new TypeReference<List<SupportCase>>() {});
            cases.addAll(loadedCases);

            log.info(">>> Успешно загружено кейсов из датасета: {}", cases.size());
        } catch (Exception thrown) {
            log.error("Ошибка при чтении dataset.json: {}", thrown.getMessage());
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

        return llmService.analyzeAggression(input);
    }

    public String checkDatasetMatch(final String userInput) {
        if (cases.isEmpty()) {
            return "Датасет не загружен.";
        }

        return cases.stream()
                .filter(c -> c.supportResponse().toLowerCase().contains(userInput.toLowerCase().trim())
                        || userInput.toLowerCase().trim().contains(c.supportResponse().toLowerCase()))
                .findFirst()
                .map(c -> """

                        [ДАТАСЕТ] Найдено похожее совпадение (ID: %d)
                        Тип: %s | Агрессия: %d%%
                        Рекомендация: %s""".formatted(
                        c.id(),
                        c.type(),
                        c.expectedMetrics().aggressionScore(),
                        c.recommendation()))
                .orElse("Прямых совпадений в датасете не найдено.");
    }

    private String buildFallbackResponse() {
        return """
                Уровень агрессии: 0%%;
                Краткая рекомендация: LLM-сервис временно недоступен.
                """;
    }
}
