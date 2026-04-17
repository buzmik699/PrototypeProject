package buzmakov.Prototype;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record SupportCase(
    int id,

    @NonNull
    String type,

    @NonNull
    @JsonProperty("analysis_layer")
    String analysisLayer,

    @NonNull
    @JsonProperty("client_message")
    String clientMessage,

    @NonNull
    @JsonProperty("support_response")
    String supportResponse,

    @NonNull
    @JsonProperty("expected_metrics")
    Metrics expectedMetrics,

    @NonNull
    @JsonProperty("recommendation_id")
    String recommendationId) {

    @Builder
    public record Metrics(
        @JsonProperty("aggression_score")
        int aggressionScore,

        @JsonProperty("informativeness_score")
        int informativenessScore,

        @NonNull
        String[] markers) {

    }
}