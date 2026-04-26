/*
package buzmakov.Prototype;

import buzmakov.Prototype.mappers.RedmineIssueMapper;
import buzmakov.Prototype.model.SupportCase;
import buzmakov.Prototype.service.FeedbackService;
import buzmakov.Prototype.services.RedmineService;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//@Component
@Profile("!test")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
@Slf4j
public class ConsoleAppRunner implements CommandLineRunner {

    TextAnalyzer textAnalyzer;
    FeedbackService feedbackService;
    RedmineService redmineService;
    RedmineIssueMapper redmineIssueMapper; // Внедряем маппер

    @Override
    public void run(final String... args) {
        val scanner = new Scanner(System.in);

        log.info("===============================================");
        log.info(">>> ЗАПУСК СИСТЕМЫ ИИ-МОДЕРАЦИИ (ФИЛЬТР: NEW)");
        log.info("===============================================");

        try {
            // 1. Получаем ТОЛЬКО необработанные задачи (где cf_1 пустое)
            val issues = redmineService.getUnprocessedIssues();

            if (issues.isEmpty()) {
                log.info(">>> Новых (необработанных) задач не найдено. Проверка завершена.");
                return;
            }

            log.info(">>> Найдено новых задач для анализа: {}", issues.size());

            for (val issue : issues) {
                // 2. Превращаем задачу Redmine в наш рекорд SupportCase
                SupportCase currentCase = redmineIssueMapper.toModel(issue);

                log.info("\n--- АНАЛИЗ ТИКЕТА #{}: \"{}\" ---", currentCase.id(), currentCase.subject());
                log.info("Содержимое: {}", currentCase.text());

                // 3. Каскадный анализ
                log.info("[СЛОЙ 1] Проверка локальными правилами...");
                var analysisResult = textAnalyzer.analyzeRuleBased(currentCase.text());

                // Проверяем, нужно ли подключать LLM (Слой 2)
                val reportString = analysisResult.getReport().toString();
                val foundInDataset = reportString.contains("баз") || reportString.contains("Совпадение");

                if (!foundInDataset && analysisResult.getScore() <= 70) {
                    log.info("[СЛОЙ 2] Локальных совпадений нет. Запрос к LLM...");
                    val llmResult = textAnalyzer.analyzeWithLlm(currentCase.text());

                    // Конвертируем ответ LLM в общий формат результата
                    analysisResult = textAnalyzer.convertLlmToAnalysisResult(llmResult);
                    log.info("\n{}", llmResult.formattedResponse());
                } else {
                    log.info(">>> ВЕРДИКТ СФОРМИРОВАН ЛОКАЛЬНО");
                }

                log.info("ИТОГ: Категория: {}, Агрессия: {}%",
                    analysisResult.getCategory(), analysisResult.getScore());

                // 4. Блок подтверждения и обучения (Feedback Loop)
                System.out.print("Вердикт верный? (y/n): ");
                val confirm = scanner.nextLine();

                String finalCategory = analysisResult.getCategory();
                int finalScore = analysisResult.getScore();

                if ("n".equalsIgnoreCase(confirm)) {
                    System.out.print("Введите правильный ID категории (напр. PA-01): ");
                    finalCategory = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Введите правильный % агрессии: ");
                    try {
                        finalScore = Integer.parseInt(scanner.nextLine().trim());
                        // Сохраняем для будущего дообучения
                        feedbackService.saveFeedback(currentCase.text(), finalCategory, finalScore);
                    } catch (Exception e) {
                        log.warn("Ошибка ввода. Используем расчетные данные.");
                    }
                }

                // 5. СОХРАНЕНИЕ В REDMINE
                // После этого вызова задача перестанет попадать в getUnprocessedIssues()
                log.info(">>> Синхронизация с Redmine...");
                redmineService.updateIssueAnalysis(currentCase.id(), finalCategory, finalScore);
                log.info("-----------------------------------------------");
            }

            log.info("\n>>> Обработка всех новых задач завершена.");

        } catch (Exception e) {
            log.error("Критическая ошибка в работе конвейера: {}", e.getMessage());
        }
    }
}*/
