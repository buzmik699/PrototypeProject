package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import buzmakov.Prototype.SupportCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DatasetMatchRule implements AnalysisRule {

    final List<SupportCase> dataset = new ArrayList<>();

    @PostConstruct
    public void init() throws Exception {
        try (val is = getClass().getResourceAsStream("/dataset_extended.json")) {
            if (is == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл dataset_extended.json не найден!");
                return;
            }
            val mapper = new ObjectMapper();
            val loadedCases = mapper.readValue(is, new TypeReference<List<SupportCase>>() {});
            dataset.addAll(loadedCases);
            log.info(">>> DatasetMatchRule: Успешно загружено кейсов: {}", dataset.size());
        } catch (Exception thrown) {
            log.error("Ошибка при чтении dataset_extended.json: {}", thrown.getMessage());
            throw thrown;
        }
    }

    @Override
    public void apply(@NonNull String input, @NonNull AnalysisResult result) {
        if (result.getScore() >= 100) return;

        String normalizedInput = input.trim().toLowerCase();

        for (val scenario : dataset) {
            String scenarioText = scenario.supportResponse().trim().toLowerCase();
            double similarity = calculateSimilarity(normalizedInput, scenarioText);

            if (similarity > 0.85) {
                int matchPercentage = (int) (similarity * 100);
                log.info("[DatasetMatchRule] Найдено совпадение: {} ({}%)", scenario.recommendationId(), matchPercentage);

                int penaltyPoints = scenario.expectedMetrics().aggressionScore();
                String reason = String.format("Совпадение с базой (ID: %s, Сходство: %d%%)",
                    scenario.recommendationId(), matchPercentage);

                result.addPenalty(penaltyPoints, reason);
                return;
            }
        }
    }

    @Override
    public String getName() {
        return "Dataset Fuzzy Match Rule";
    }

    private double calculateSimilarity(String s1, String s2) {
        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / Math.max(s1.length(), s2.length()));
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}