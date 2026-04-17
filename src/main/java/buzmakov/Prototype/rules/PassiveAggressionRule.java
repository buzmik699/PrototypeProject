package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PassiveAggressionRule implements AnalysisRule {

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        // Используем StringUtils для удаления ведущих пробелов согласно стандартам
        if (StringUtils.stripStart(input, null).startsWith("...")) {
            result.addPenalty(30, "Пассивная агрессия: многоточие в начале фразы");
        }
    }

    @Override
    public String getName() {
        return "PassiveAggressionRule";
    }
}