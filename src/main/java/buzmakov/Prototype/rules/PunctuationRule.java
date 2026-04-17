package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PunctuationRule implements AnalysisRule {

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        if (input.matches("(?s).*[?!]{2,}.*")) {
            result.addPenalty(25, "Избыточное использование знаков препинания (давление)");
        }
    }

    @Override
    public String getName() {
        return "PunctuationRule";
    }
}