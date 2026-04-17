package buzmakov.Prototype.service;

import lombok.NonNull;

public interface LlmService {

    @NonNull
    String analyzeAggression(@NonNull final String input);

    boolean isAvailable();
}