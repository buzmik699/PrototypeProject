package buzmakov.Prototype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class PrototypeApplication implements CommandLineRunner {

    // Модификатор private подставится из lombok.config
    final TextAnalyzer textAnalyzer;

    public static void main(final String[] args) {
        SpringApplication.run(PrototypeApplication.class, args);
    }

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

            if (input.equalsIgnoreCase("exit")) {
                log.info("Завершение работы...");
                break;
            }

            if (input.isBlank()) {
                continue;
            }

            log.info("\n--- ЗАПУСК КАСКАДНОГО АНАЛИЗА ---");

            // ЭТАП 1: Локальный анализ (Правила + Историческая база Kaggle)
            val rules = textAnalyzer.analyzeRuleBased(input);

            if (log.isInfoEnabled()) {
                log.info("[СЛОЙ 1] Уровень агрессии: {}%", rules.getScore());
            }

            // Если первый слой что-то нашел (мат, капс или совпадение в базе) - выводим отчет
            if (!rules.getReport().isEmpty()) {
                log.info("Отчет локального анализа:\n{}", rules.getReport().toString());
            }

            // Блокировка при откровенном хамстве (StopWordsRule)
            if (rules.getScore() > 70) {
                log.warn(">>> ВЕРДИКТ: БЛОКИРОВКА. Текст грубо нарушает этические нормы.");
                log.info("---------------------------------");
                continue;
            }

            // Если отработал DatasetMatchRule, мы можем не дергать нейросеть
            // Проверяем по тексту отчета, было ли совпадение с базой
            boolean foundInDataset = rules.getReport().toString().contains("баз")
                || rules.getReport().toString().contains("Совпадение");

            if (foundInDataset) {
                log.info(">>> ВЕРДИКТ: Категория определена локально по исторической базе. LLM не требуется.");
                log.info("---------------------------------");
                continue;
            }

            // ЭТАП 2: Нейросеть (LLM)
            log.info("[СЛОЙ 2] Точных совпадений нет. Запуск семантического анализа LLM...");

            val llmResult = textAnalyzer.analyzeWithLlm(input);
            log.info(llmResult);

            log.info("---------------------------------");
        }
    }
}