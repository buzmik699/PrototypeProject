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
        val path = "/russianBadWords/russian_bad_words.json";
        try (val inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                log.error("Критическая ошибка: файл справочника стоп-слов не найден: {}", path);
                return;
            }
            val badWords = mapper.readValue(inputStream, new TypeReference<List<String>>() {});
            allStopWords.addAll(badWords);
        } catch (final Exception thrown) {
            log.error("Не удалось загрузить стоп-слова: {}", thrown.getMessage());
        }
    }

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        val lowerInput = input.toLowerCase();

        allStopWords.stream()
            .filter(word -> {
                // Используем границы, учитывающие специфику Unicode (буквы и цифры)
                val regex = "(?<![\\p{L}\\p{N}])" + Pattern.quote(word.toLowerCase()) + "(?![\\p{L}\\p{N}])";
                val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
                return pattern.matcher(lowerInput).find();
            })
            .findFirst()
            .ifPresent(word -> {
                // Добавляем слово "база", чтобы RedmineMonitor увидел, что сработал 1-й слой
                result.addPenalty(100, "Найдено в базе стоп-слов: \"%s\"".formatted(word));
                // Сразу ставим категорию агрессии
                result.setCategory("AG-01");
                result.setRecommendation("Полностью пересмотрите формулировку. " +
                    "Исключите любые эмоциональные выпады и перепишите сообщение в строгом, " +
                    "корректном и уважительном официально-деловом стиле.");
            });
    }

    @Override
    public String getName() {
        return "StopWordsRule";
    }
}