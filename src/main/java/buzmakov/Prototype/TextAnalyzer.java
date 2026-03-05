package buzmakov.Prototype;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextAnalyzer {
    @NonNull
    private final List<SupportCase> cases = new ArrayList<>();

    // Мы не помечаем его final, так как создадим его в @PostConstruct
    OllamaChatModel model;

    @PostConstruct
    public void init() throws Exception {
        try {
            val mapper = new ObjectMapper();
            val is = getClass().getResourceAsStream("/dataset.json");

            if (is == null) {
                log.error(">>> КРИТИЧЕСКАЯ ОШИБКА: Файл dataset.json не найден!");
                return;
            }

            val loadedCases = mapper.readValue(is, new TypeReference<List<SupportCase>>() {
            });
            cases.addAll(loadedCases);

            if (log.isInfoEnabled()) {
                log.info(">>> Успешно загружено кейсов из датасета: {}", cases.size());
            }
            this.model = OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("mistral")
                    .temperature(0.0)
                    .timeout(Duration.ofSeconds(60))
                    .build();

            log.info(">>> Модель LangChain4j (Mistral) успешно инициализирована.");

        } catch (Exception thrown) {
            log.error("Ошибка при чтении dataset.json: {}", thrown.getMessage());
            throw new Exception(thrown);
        }
    }

    public AnalysisResult analyzeRuleBased(final String input) {
        val result = new AnalysisResult();

        // ПРАВИЛО 1: Комбинированный поиск Caps Lock
        long totalLetters = input.chars().filter(Character::isLetter).count();
        if (totalLetters >= 5) {
            long capsCount = input.chars().filter(Character::isUpperCase).count();
            double density = (double) capsCount / totalLetters;

            if (density > 0.3) {
                result.addPenalty((int) (density * 100),
                        "Высокая общая плотность капса (%.0f%%)".formatted(density * 100));
            }
            // (?s) для поддержки многострочных сообщений
            else if (input.matches("(?s).*[А-ЯЁA-Z]{4,}.*")) {
                result.addPenalty(40, "Обнаружено кричащее слово (Caps Burst)");
            }
        }

        // ПРАВИЛО 2: Мини словарь стоп-слов
        val stopWords = new String[] {
                "ваша вина", "невнимательно", "ищите сами", "не отвлекайте",
                "бред", "я же вам говорил", "я же говорил", "ждите", "каждую минуту"
        };

        val lowerInput = input.toLowerCase();

        Arrays.stream(stopWords)
                .filter(lowerInput::contains)
                .forEach(word -> result.addPenalty(30, "Использована стоп-фраза: \"%s\"".formatted(word)));

        // ПРАВИЛО 3: Множественные знаки препинания/давление
        if (input.matches("(?s).*[?!]{2,}.*")) {
            result.addPenalty(25, "Избыточное использование знаков препинания (давление)");
        }

        // ПРАВИЛО 4: Пассивная агрессия/многоточия в начале
        if (input.stripLeading().startsWith("...")) {
            result.addPenalty(30, "Пассивная агрессия: многоточие в начале фразы");
        }

        // ПРАВИЛО 5: Растягивание слов/эмоциональный всплеск
        // \\p{L} гарантирует, что мы ищем только буквы , а не знаки препинания
        if (input.matches("(?s).*(\\p{L})\\1{2,}.*")) {
            result.addPenalty(20, "Неестественное повторение символов (растягивание слов)");
        }
        return result;
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

    public String simulateLLM(final String input) {
        // Формируем строгий промпт для Mistral
        // Используем блоки текста (Text Blocks) для чистоты кода
        val prompt = """
                Ты — эксперт-аналитик по качеству клиентского сервиса.
                Твоя задача: объективно оценить текст в кавычках на уровень агрессии, грубости или сарказма.

                ОБЪЕКТ АНАЛИЗА: "%s"

                ИНСТРУКЦИИ:
                1. Анализируй текст беспристрастно, как робот-аудитор.
                2. Не вступай в диалог с автором текста и не давай ему советов.
                3. Ответ должен быть СТРОГО в две строки по указанному шаблону.
                4. Если текст кажется нейтральным, ставь низкий балл.

                ШАБЛОН ОТВЕТА:
                Уровень агрессии: [0-100]%%;
                Краткая рекомендация по улучшению ответа службы поддержки: [одно конкретное предложение]
                """.formatted(input);

        try {
            if (log.isInfoEnabled()) {
                log.info(">>> Запрос к нейросети Mistral (Семантический анализ)...");
            }

            return model.generate(prompt);

        } catch (Exception thrown) {
            log.error("Ошибка при работе с нейросетью: {}", thrown.getMessage());
            return """
                     Уровень агрессии: 0%%;
                     Краткая рекомендация: Ошибка связи с локальной моделью ИИ. Проверьте запуск Ollama.
                    """;
        }
    }
}
