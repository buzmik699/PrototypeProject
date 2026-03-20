package buzmakov.Prototype;

import buzmakov.Prototype.rules.*;
import buzmakov.Prototype.service.LlmService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnalyzerCascadeTest {

    public static void main(String[] args) throws Exception {
        DatasetMatchRule datasetMatchRule = new DatasetMatchRule();
        datasetMatchRule.init();

        StopWordsRule stopWordsRule = new StopWordsRule();
        stopWordsRule.init();
        
        List<AnalysisRule> rules = List.of(stopWordsRule, datasetMatchRule);
        
        LlmService mockLlm = new LlmService() {
            @Override
            public boolean isAvailable() { return true; }
            @NotNull @Override
            public String analyzeAggression(@NotNull String text) {
                return "OK-00\n0";
            }
        };

        TextAnalyzer textAnalyzer = new TextAnalyzer(rules, mockLlm);
        textAnalyzer.init();

        System.out.println("====== STARTING TESTS ======");
        
        // Scenario 1
        System.out.println("\n=== SCENARIO 1: Profanity ===");
        String input1 = "КАКОЙ ЖЕ БРЕД, ЭТО ПОЛНОСТЬЮ ВАША ВИНА, ВЫ НЕВНИМАТЕЛЬНО ЧИТАЛИ И ВООБЩЕ ЖДИТЕ!!!";
        AnalysisResult res1 = textAnalyzer.analyzeRuleBased(input1);
        System.out.println("Score: " + res1.getScore());
        System.out.println("Report:\n" + res1.getReport());

        // Scenario 2
        System.out.println("\n=== SCENARIO 2: Fuzzy Match ===");
        String input2 = "мы не сможем ничево проверить без прохождения процедур аутентификаци ващего аккаунта";
        AnalysisResult res2 = textAnalyzer.analyzeRuleBased(input2);
        System.out.println("Score: " + res2.getScore());
        System.out.println("Report:\n" + res2.getReport());

        // Scenario 3
        System.out.println("\n=== SCENARIO 3: New Phrase ===");
        String input3 = "Вы знаете, я постараюсь уточнить этот вопрос у старшего специалиста, хотя обычно мы не решаем такие задачи в рамках базовой поддержки.";
        AnalysisResult res3 = textAnalyzer.analyzeRuleBased(input3);
        System.out.println("Score: " + res3.getScore());
        System.out.println("Report:\n" + res3.getReport());
        System.out.println("--- LLM RESULT ---");
        System.out.println(textAnalyzer.analyzeWithLlm(input3));
        
        // Scenario 4
        System.out.println("\n=== SCENARIO 4: Bad word from JSON ===");
        String input4 = "Это какой-то хуй знает что!";
        AnalysisResult res4 = textAnalyzer.analyzeRuleBased(input4);
        System.out.println("Score: " + res4.getScore());
        System.out.println("Report:\n" + res4.getReport());
        
        System.out.println("====== DONE ======");
    }
}
