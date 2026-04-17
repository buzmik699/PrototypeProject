package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(makeFinal = true)
public class CapsLockRule implements AnalysisRule {

    static double CAPS_DENSITY_THRESHOLD = 0.3;

    static int MIN_TOTAL_LETTERS = 5;

    static int CAPS_BURST_MIN_LENGTH = 4;

    @Override
    public void apply(@NonNull final String input, @NonNull final AnalysisResult result) {
        long totalLetters = input.chars()
            .filter(Character::isLetter)
            .count();

        if (totalLetters < MIN_TOTAL_LETTERS) {
            return;
        }

        long capsCount = input.chars()
            .filter(Character::isUpperCase)
            .count();

        double density = (double) capsCount / totalLetters;

        if (density > CAPS_DENSITY_THRESHOLD) {
            int penalty = (int) (density * 100);
            result.addPenalty(
                penalty,
                "Высокая общая плотность капса (%.0f%%)".formatted(density * 100)
            );
        } else if (input.matches("(?s).*[А-ЯЁA-Z]{" + CAPS_BURST_MIN_LENGTH + ",}.*")) {
            result.addPenalty(40, "Обнаружено кричащее слово (Caps Burst)");
        }
    }

    @Override
    public String getName() {
        return "CapsLockRule";
    }
}