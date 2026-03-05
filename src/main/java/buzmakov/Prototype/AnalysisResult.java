package buzmakov.Prototype;

import lombok.Data;
import lombok.NonNull;

@Data
public class AnalysisResult {
    int score; // Уровень агрессии (0-100)
    @NonNull
    StringBuilder report; // Текстовое пояснение (почему такой балл)

    public AnalysisResult() {
        this.score = 0;
        this.report = new StringBuilder();
    }

    public void addPenalty(int points, String reason) {
        this.score += points;
        this.report.append("- [").append(points).append("%] ").append(reason).append("\n");
        // Ограничиваем, чтобы не вылезло за 100%
        if (this.score > 100)
            this.score = 100;
    }
}