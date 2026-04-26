package buzmakov.Prototype.model;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record SupportCase(
    @NonNull
    Integer id,

    @NonNull
    String subject,
    String text,
    String aiCategory,
    Integer aggressionScore,
    String recommendation,

    @NonNull
    AuthorRole role
) {}