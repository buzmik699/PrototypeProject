package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(makeFinal = true)
@Slf4j
public class StopWordsRule implements AnalysisRule {

    List<String> allStopWords = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try (val inputStream = getClass().getResourceAsStream("/russianBadWords/russian_bad_words.json")) {
            if (inputStream == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл russian_bad_words.json не найден!");
                return;
            }

            val badWords = mapper.readValue(
                inputStream,
                new TypeReference<List<String>>() {
                });

            allStopWords.addAll(badWords);

            if (log.isInfoEnabled()) {
                log.info("Загружено {} стоп-слов из JSON", badWords.size());
            }
        } catch (Exception thrown) {
            log.error("Не удалось загрузить стоп-слова из JSON", thrown);
        }
    }

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        val lowerInput = input.toLowerCase();

        allStopWords.forEach(word -> {
            val regex = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
            val pattern = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
            );

            if (pattern.matcher(lowerInput).find()) {
                result.addPenalty(
                    100,
                    "Использована стоп-фраза: \"%s\"".formatted(word)
                );
            }
        });
    }

    @Override
    public String getName() {
        return "StopWordsRule";
    }
}