package buzmakov.Prototype;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Slf4j
class AggressionBenchmarkTest {

    TextAnalyzer service;

    @Test
    void runQualityBenchmark() throws Exception {
        val mapper = new ObjectMapper();
        val resource = new ClassPathResource("benchmark-cases.json");

        val cases = mapper.readValue(
            resource.getInputStream(),
            new TypeReference<List<TestCase>>() {
            });

        log.info("==========================================");
        log.info("=== ЗАПУСК БЕНЧМАРКА КАЧЕСТВА СИСТЕМЫ ===");
        log.info("==========================================");

        val correctPredictions = cases.stream()
            .filter(testCase -> {
                val result = service.analyzeWithLlm(testCase.text());

                val isCategoryCorrect = result.categoryId().equals(testCase.expectedCategory());
                val scoreDiff = Math.abs(result.score() - testCase.expectedAggression());
                val isScoreCorrect = scoreDiff <= 15;

                val isCorrect = isCategoryCorrect && isScoreCorrect;

                log.info("Тест: [{}]", testCase.description());
                log.info("   Ожидалось: Категория={}, Агрессия={}%",
                    testCase.expectedCategory(),
                    testCase.expectedAggression());

                log.info("   Получено:  Категория={}, Агрессия={}%",
                    result.categoryId(),
                    result.score());

                val status = isCorrect
                    ? "✅ УСПЕШНО"
                    : "❌ ОШИБКА";

                log.info("   Статус:    {}", status);
                log.info("------------------------------------------");

                return isCorrect;
            })
            .count();

        val accuracy = (double) correctPredictions / cases.size() * 100;

        if (log.isInfoEnabled()) {
            log.info("==========================================");
            log.info("ИТОГО ПРОЙДЕНО: {} из {}", correctPredictions, cases.size());
            log.info("ИТОГОВАЯ ТОЧНОСТЬ СИСТЕМЫ: {}%", "%.1f".formatted(accuracy));
            log.info("==========================================");
        }

        assertTrue(accuracy > 80.0, "Точность классификации слишком низкая! Требуется настройка промпта или модели.");
    }

    public record TestCase(
        String text,
        String expectedCategory,
        int expectedAggression,
        String description) {

    }
}