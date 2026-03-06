package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StopWordsRule implements AnalysisRule {
    static final String[] STOP_WORDS = {
        "ваша вина", "невнимательно", "ищите сами", "не отвлекайте",
        "бред", "я же вам говорил", "я же говорил", "ждите", "каждую минуту"
    };

    @Override
    public void apply(@NonNull String input, @NonNull AnalysisResult result) {
        String lowerInput = input.toLowerCase();
        Arrays.stream(STOP_WORDS)
            .filter(lowerInput::contains)
            .forEach(word -> result.addPenalty(30,
                "Использована стоп-фраза: \"%s\"".formatted(word)));
    }

    @Override
    public String getName() { return "StopWordsRule"; }
}
