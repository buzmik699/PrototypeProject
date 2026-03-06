package buzmakov.Prototype.service;

import lombok.NonNull;

public interface LlmService {
    @NonNull
    String analyzeAggression(@NonNull String input);
    boolean isAvailable();
}
