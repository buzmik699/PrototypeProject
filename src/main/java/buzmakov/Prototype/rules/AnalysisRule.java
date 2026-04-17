package buzmakov.Prototype.rules;

import buzmakov.Prototype.AnalysisResult;
import lombok.NonNull;

public interface AnalysisRule {

    void apply(@NonNull final String input, @NonNull final AnalysisResult result);

    String getName();
}