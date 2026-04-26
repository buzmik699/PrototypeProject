package buzmakov.Prototype.services;

import buzmakov.Prototype.TextAnalyzer;
import buzmakov.Prototype.mappers.RedmineIssueMapper;
import buzmakov.Prototype.model.SupportCase;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
@Slf4j
public class RedmineMonitor {

    RedmineService redmineService;
    TextAnalyzer textAnalyzer;
    RedmineIssueMapper redmineIssueMapper;

    @Scheduled(fixedDelay = 30000)
    public void checkNewIssues() {
        log.info(">>> [ФОН] Проверка новых задач в Redmine...");

        try {
            val issues = redmineService.getUnprocessedIssues();

            if (issues.isEmpty()) {
                return;
            }

            log.info(">>> [ФОН] Найдено новых задач: {}", issues.size());

            issues.forEach(issue -> {
                val currentCase = redmineIssueMapper.toModel(issue);

                val textToAnalyze = ofNullable(currentCase.text())
                    .filter(StringUtils::isNotBlank)
                    .orElseGet(() -> ofNullable(currentCase.subject()).orElse(""));

                var result = textAnalyzer.analyzeRuleBased(textToAnalyze, currentCase.role());

                if (!result.getReport().toString().contains("баз") && result.getScore() <= 70) {
                    val llmResult = textAnalyzer.analyzeWithLlm(textToAnalyze, currentCase.role());
                    result = textAnalyzer.convertLlmToAnalysisResult(llmResult);
                }

                redmineService.updateIssueAnalysis(
                    currentCase.id(),
                    result.getCategory(),
                    result.getScore(),
                    result.getRecommendation());

                log.info(">>> [ФОН] Тикет #{} обработан автоматически: {} ({}%). Рекомендация добавлена.",
                    currentCase.id(), result.getCategory(), result.getScore());
            });
        } catch (final Exception thrown) {
            log.error(">>> [ФОН] Ошибка мониторинга: {}", thrown.getMessage());
        }
    }
}