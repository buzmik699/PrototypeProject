package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StopWordsRule implements AnalysisRule {
    static final String[] STATIC_STOP_WORDS = {
        "ваша вина", "невнимательно", "ищите сами", "не отвлекайте",
        "бред", "я же вам говорил", "я же говорил", "ждите", "каждую минуту"
    };


    final List<String> allStopWords = new ArrayList<>();

    @PostConstruct
    public void init() {
        allStopWords.addAll(List.of(STATIC_STOP_WORDS));
        
        try (val is = getClass().getResourceAsStream("/russianBadWords/russian_bad_words.json")) {
            if (is == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл russian_bad_words.json не найден!");
                return;
            }
            val mapper = new ObjectMapper();
            List<String> badWords = mapper.readValue(is, new TypeReference<>() {});
            allStopWords.addAll(badWords);
            log.info("Loaded {} bad words from JSON", badWords.size());
        } catch (Exception e) {
            log.error("Failed to load bad words from JSON", e);
        }
    }

    @Override
    public void apply(@NonNull String input, @NonNull AnalysisResult result) {
        String lowerInput = input.toLowerCase();
        allStopWords.stream()
            .filter(lowerInput::contains)
            .forEach(word -> result.addPenalty(100,
                "Использована стоп-фраза: \"%s\"".formatted(word)));
    }

    @Override
    public String getName() { return "StopWordsRule"; }
}
