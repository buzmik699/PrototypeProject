package buzmakov.Prototype.service;

import buzmakov.Prototype.model.AuthorRole;
import lombok.NonNull;

public interface LlmService {

    @NonNull
    String analyzeAggression(String input, AuthorRole role);

    boolean isAvailable();
}