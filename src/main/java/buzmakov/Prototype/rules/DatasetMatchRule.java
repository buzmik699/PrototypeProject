package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import buzmakov.Prototype.SupportCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@FieldDefaults(makeFinal = true)
public class DatasetMatchRule implements AnalysisRule {

    List<SupportCase> dataset = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        try (val inputStream = getClass().getResourceAsStream("/dataset_extended.json")) {
            if (inputStream == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл dataset_extended.json не найден!");
                return;
            }

            val loadedCases = mapper.readValue(
                inputStream,
                new TypeReference<List<SupportCase>>() {
                }
            );
            dataset.addAll(loadedCases);

            if (log.isInfoEnabled()) {
                log.info(">>> DatasetMatchRule: Успешно загружено кейсов: {}", dataset.size());
            }
        } catch (Exception thrown) {
            log.error("Ошибка при чтении dataset_extended.json: {}", thrown.getMessage());
            throw thrown;
        }
    }

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        if (result.getScore() >= 100) {
            return;
        }

        String normalizedInput = StringUtils.trim(input).toLowerCase();

        dataset.stream()
            .filter(scenario -> {
                String scenarioText = StringUtils.trim(scenario.supportResponse()).toLowerCase();
                return calculateSimilarity(normalizedInput, scenarioText) > 0.85;
            })
            .findFirst()
            .ifPresent(scenario -> {
                String scenarioText = StringUtils.trim(scenario.supportResponse()).toLowerCase();
                double similarity = calculateSimilarity(normalizedInput, scenarioText);
                int matchPercentage = (int) (similarity * 100);

                if (log.isInfoEnabled()) {
                    log.info("[DatasetMatchRule] Найдено совпадение: {} ({}%)",
                        scenario.recommendationId(),
                        matchPercentage);
                }

                int penaltyPoints = scenario.expectedMetrics().aggressionScore();
                String reason = "Совпадение с базой (ID: %s, Сходство: %d%%)"
                    .formatted(scenario.recommendationId(), matchPercentage);

                result.addPenalty(penaltyPoints, reason);
            });
    }

    @Override
    public String getName() {
        return "Dataset Fuzzy Match Rule";
    }

    private double calculateSimilarity(final String s1, final String s2) {
        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / Math.max(s1.length(), s2.length()));
    }

    private int computeLevenshteinDistance(final String s1, final String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int indexS1 = 0; indexS1 <= s1.length(); indexS1++) {
            for (int indexS2 = 0; indexS2 <= s2.length(); indexS2++) {
                if (indexS1 == 0) {
                    dp[indexS1][indexS2] = indexS2;
                } else if (indexS2 == 0) {
                    dp[indexS1][indexS2] = indexS1;
                } else {
                    int cost = (s1.charAt(indexS1 - 1) == s2.charAt(indexS2 - 1))
                        ? 0
                        : 1;

                    dp[indexS1][indexS2] = Math.min(
                        Math.min(dp[indexS1 - 1][indexS2] + 1, dp[indexS1][indexS2 - 1] + 1),
                        dp[indexS1 - 1][indexS2 - 1] + cost
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}