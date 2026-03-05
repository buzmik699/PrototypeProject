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
	private final TextAnalyzer textAnalyzer;

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
					>>> Введите фразу для проверки (или 'exit' для выхода)
					===============================================""");
		}

		while (true) {
			System.out.print("\nВведите ответ поддержки: ");
			val input = scanner.nextLine();

			if (input.equalsIgnoreCase("exit")) {

				log.info("Завершение работы...");
				break;
			}

			if (input.isBlank()) {
				continue;
			}

			log.info("\n--- ЗАПУСК КАСКАДНОГО АНАЛИЗА ---");

			// ЭТАП 1: Быстрые правила (Rule-Based)
			val rules = textAnalyzer.analyzeRuleBased(input);

			if (log.isInfoEnabled()) {
				log.info("[СЛОЙ 1] Уровень явной агрессии: {}%", rules.getScore());
			}

			if (rules.getScore() > 70) {

				log.warn(rules.getReport().toString());
				log.warn(">>> ВЕРДИКТ: БЛОКИРОВКА. Текст нарушает этические нормы.");
				continue;
			}

			// ЭТАП 2: База знаний (Dataset)
			val datasetMatch = textAnalyzer.checkDatasetMatch(input);
			val foundInDataset = !datasetMatch.contains("не найдено");

			if (foundInDataset) {

				log.info("[СЛОЙ 2] Найдено совпадение в архиве кейсов:");
				log.info(datasetMatch);
				continue;
			}

			// ЭТАП 3: Нейросеть (LLM Simulation)
			log.info("[СЛОЙ 2] Совпадений в базе нет. Запуск семантического анализа...");

			val llmResult = textAnalyzer.simulateLLM(input);
			log.info(llmResult);

			log.info("---------------------------------");
		}
	}
}
