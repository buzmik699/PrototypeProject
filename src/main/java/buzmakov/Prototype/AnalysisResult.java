package buzmakov.Prototype;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Data
@FieldDefaults(makeFinal = true)
public class AnalysisResult {

    @NonFinal
    int score;

    @NonFinal
    String category = "OK-00";

    @NonFinal
    String recommendation = "";

    @NonNull
    StringBuilder report;

    public AnalysisResult() {
        this.score = 0;
        this.report = new StringBuilder();
    }

    public void addPenalty(final int points, final String reason) {
        this.score += points;
        this.report.append("- [%d%%] %s\n".formatted(points, reason));

        if (this.score > 100) {
            this.score = 100;
        }
    }
}