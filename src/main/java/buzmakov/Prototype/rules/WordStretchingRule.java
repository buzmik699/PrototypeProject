package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class WordStretchingRule implements AnalysisRule {
    @Override
    public void apply(@NonNull String input, @NonNull AnalysisResult result) {
        if (input.matches("(?s).*(\\p{L})\\1{2,}.*")) {
            result.addPenalty(20, "Неестественное повторение символов (растягивание слов)");
        }
    }

    @Override
    public String getName() {
        return "WordStretchingRule";
    }
}
