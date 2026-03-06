package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PassiveAggressionRule implements AnalysisRule{
    @Override
    public void apply(@NonNull String input, @NonNull AnalysisResult result) {
        if (input.stripLeading().startsWith("...")) {
            result.addPenalty(30, "Пассивная агрессия: многоточие в начале фразы");
        }
    }

    @Override
    public String getName() {
        return "PassiveAggressionRule";
    }
}
