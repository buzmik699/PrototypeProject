package buzmakov.Prototype.services;

import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true)
public class RedmineService {

    @Value("${redmine.url}")
    @NonFinal
    String redmineUrl;

    @Value("${redmine.api-key}")
    @NonFinal
    String apiKey;

    @Value("${redmine.project-key}")
    @NonFinal
    String projectKey;

    @NonFinal
    RedmineManager manager;

    @PostConstruct
    public void init() {
        this.manager = RedmineManagerFactory.createWithApiKey(redmineUrl, apiKey);

        if (log.isInfoEnabled()) {
            log.info("Сервис Redmine успешно инициализирован для адреса: {}", redmineUrl);
        }
    }

    public List<Issue> getIssues() throws Exception {
        val issueManager = manager.getIssueManager();
        return issueManager.getIssues(projectKey, null);
    }

    public List<Issue> getUnprocessedIssues() {
        try {
            val params = new HashMap<String, String>();

            params.put("project_id", projectKey);
            params.put("status_id", "open");
            params.put("cf_1", "!*");

            log.info("Запрос к Redmine для получения необработанных задач проекта: {}", projectKey);
            return manager.getIssueManager().getIssues(params).getResults();
        } catch (final Exception thrown) {
            log.error("Произошла ошибка при получении списка задач из Redmine: {}", thrown.getMessage());
            return List.of();
        }
    }

    public void testConnection() {
        try {
            val issues = getIssues();

            if (issues.isEmpty()) {
                log.info("Подключение к Redmine выполнено успешно, задачи в проекте '{}' отсутствуют.", projectKey);
            } else {
                log.info("Успешное подключение! Количество найденных задач: {}", issues.size());

                issues.forEach(issue -> {
                    log.info("Обработка задачи #{}: {} | Содержимое: {}",
                        issue.getId(),
                        issue.getSubject(),
                        issue.getDescription());
                });
            }
        } catch (final Exception thrown) {
            log.error("Критическая ошибка при попытке подключения к Redmine: {}", thrown.getMessage());
            log.error("Необходимо проверить доступность Docker-контейнеров и корректность API-ключа в конфигурационном файле.");
        }
    }

    public void updateIssueAnalysis(final Integer issueId, final String category, final int score, final String recommendation) {
        try {
            val issueManager = manager.getIssueManager();
            val issue = issueManager.getIssueById(issueId);

            issue.getCustomFields().forEach(field -> {
                val fieldName = ofNullable(field.getName()).orElse("");

                if (fieldName.equalsIgnoreCase("AI Category")) {
                    field.setValue(category);
                }

                if (fieldName.equalsIgnoreCase("Aggression Score")) {
                    field.setValue(String.valueOf(score));
                }

                if (fieldName.equalsIgnoreCase("AI Recommendation")) {
                    field.setValue(ofNullable(recommendation).orElse("Рекомендации отсутствуют"));
                }
            });

            issueManager.update(issue);
            log.info("Задача #{} успешно обновлена в Redmine. Вердикт: {} ({}%)", issueId, category, score);
        } catch (final Exception thrown) {
            log.error("Не удалось обновить данные анализа для задачи #{}: {}", issueId, thrown.getMessage());
        }
    }
}