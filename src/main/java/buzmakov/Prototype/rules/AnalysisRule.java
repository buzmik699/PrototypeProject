package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;

public interface AnalysisRule {
    void apply(@NonNull String input,
               @NonNull AnalysisResult result);
    String getName();
}
