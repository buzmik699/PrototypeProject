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

    @NonNull
    StringBuilder report;

    public AnalysisResult() {
        this.score = 0;
        this.report = new StringBuilder();
    }

    public void addPenalty(final int points, final String reason) {
        this.score += points;

        this.report.append("- [")
            .append(points)
            .append("%] ")
            .append(reason)
            .append("\n");

        if (this.score > 100) {
            this.score = 100;
        }
    }
}