package buzmakov.Prototype;

import buzmakov.Prototype.service.FeedbackService;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
@Slf4j
public class ConsoleAppRunner implements CommandLineRunner {

    TextAnalyzer textAnalyzer;

    FeedbackService feedbackService;

    @Override
    public void run(final String... args) {
        val scanner = new Scanner(System.in);

        if (log.isInfoEnabled()) {
            log.info("""

                    ===============================================
                    >>> СИСТЕМА ГИБРИДНОГО АНАЛИЗА ЗАПУЩЕНА
                    >>> Введите ответ поддержки (или 'exit' для выхода)
                    ===============================================""");
        }

        while (true) {
            System.out.print("\nВвод: ");
            val input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                log.info("Завершение работы...");
                break;
            }

            if (StringUtils.isBlank(input)) {
                continue;
            }

            log.info("\n--- ЗАПУСК КАСКАДНОГО АНАЛИЗА ---");

            val rules = textAnalyzer.analyzeRuleBased(input);

            if (log.isInfoEnabled()) {
                log.info("[СЛОЙ 1] Уровень агрессии: {}%", rules.getScore());
            }

            if (!rules.getReport().isEmpty()) {
                log.info("Отчет локального анализа:\n{}", rules.getReport());
            }

            if (rules.getScore() > 70) {
                log.warn(">>> ВЕРДИКТ: БЛОКИРОВКА. Текст грубо нарушает этические нормы.");
                log.info("---------------------------------");
                continue;
            }

            val reportString = rules.getReport().toString();
            val foundInDataset = reportString.contains("баз")
                || reportString.contains("Совпадение");

            if (foundInDataset) {
                log.info(">>> ВЕРДИКТ: Категория определена локально по исторической базе. LLM не требуется.");
                log.info("---------------------------------");
                continue;
            }

            log.info("[СЛОЙ 2] Точных совпадений нет. Запуск семантического анализа LLM...");

            val llmResult = textAnalyzer.analyzeWithLlm(input);

            // Исправлено: обращение к полю record без префикса get
            log.info("\n{}", llmResult.formattedResponse());

            System.out.print("Результат верный? (y/n): ");
            val confirm = scanner.nextLine();

            if (!"n".equalsIgnoreCase(confirm)) {
                log.info("---------------------------------");
                continue;
            }

            System.out.print("Введите правильный ID категории (например, PA-01): ");
            val correctCat = scanner.nextLine().trim().toUpperCase();

            System.out.print("Введите правильный % агрессии: ");

            try {
                val correctScore = Integer.parseInt(scanner.nextLine().trim());
                feedbackService.saveFeedback(input, correctCat, correctScore);
            } catch (NumberFormatException thrown) {
                log.error(">>> Ошибка: Нужно было ввести число. Обучение пропущено.");
            }

            log.info("---------------------------------");
        }
    }
}