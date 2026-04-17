package buzmakov.Prototype.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true)
@Slf4j
public class FeedbackService {

    static String FILE_PATH = "src/main/resources/user_feedback.json";

    @NonFinal
    List<FeedbackItem> feedbackList = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            val file = new File(FILE_PATH);

            if (file.exists()) {
                feedbackList = mapper.readValue(file, new TypeReference<List<FeedbackItem>>() {
                });
            }
        } catch (Exception thrown) {
            log.error("Ошибка загрузки фидбека: {}", thrown.getMessage());
        }
    }

    public void saveFeedback(final String text, final String category, final int score) {
        feedbackList.add(new FeedbackItem(text, category, score));

        try {
            mapper.writeValue(new File(FILE_PATH), feedbackList);
            log.info(">>> Фидбек сохранен и учтен!");
        } catch (Exception thrown) {
            log.error("Не удалось сохранить файл: {}", thrown.getMessage());
        }
    }

    public Optional<FeedbackItem> findMatch(final String input) {
        val processedInput = StringUtils.trim(input);

        val exactMatch = feedbackList.stream()
            .filter(item -> StringUtils.trim(item.text()).equalsIgnoreCase(processedInput))
            .findFirst();

        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        return feedbackList.stream()
            .filter(item -> {
                val similarity = calculateSimilarity(
                    processedInput.toLowerCase(),
                    StringUtils.trim(item.text()).toLowerCase()
                );

                if (log.isDebugEnabled() && similarity > 0.7) {
                    log.debug("Сравнение: [{}] и [{}]. Сходство: {}%",
                        processedInput,
                        item.text(),
                        Math.round(similarity * 100));
                }

                return similarity >= 0.90;
            })
            .max((item1, item2) -> Double.compare(
                calculateSimilarity(processedInput.toLowerCase(), item1.text().toLowerCase()),
                calculateSimilarity(processedInput.toLowerCase(), item2.text().toLowerCase())
            ));
    }

    private double calculateSimilarity(final String s1, final String s2) {
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }

        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        val distance = calculateLevenshteinDistance(s1, s2);
        val maxLength = Math.max(s1.length(), s2.length());

        return 1.0 - ((double) distance / maxLength);
    }

    private int calculateLevenshteinDistance(final String s1, final String s2) {
        val dp = new int[s1.length() + 1][s2.length() + 1];

        for (int indexS1 = 0; indexS1 <= s1.length(); indexS1++) {
            dp[indexS1][0] = indexS1;
        }

        for (int indexS2 = 0; indexS2 <= s2.length(); indexS2++) {
            dp[0][indexS2] = indexS2;
        }

        for (int indexS1 = 1; indexS1 <= s1.length(); indexS1++) {
            for (int indexS2 = 1; indexS2 <= s2.length(); indexS2++) {
                val cost = (s1.charAt(indexS1 - 1) == s2.charAt(indexS2 - 1))
                    ? 0
                    : 1;

                dp[indexS1][indexS2] = Math.min(
                    Math.min(dp[indexS1 - 1][indexS2] + 1, dp[indexS1][indexS2 - 1] + 1),
                    dp[indexS1 - 1][indexS2 - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public record FeedbackItem(
        String text,
        String category,
        int score) {
    }
}