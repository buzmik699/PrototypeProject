package buzmakov.Prototype;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record SupportCase(
        int id,

        @NonNull
        String type,

        @JsonProperty("analysis_layer")
        @NonNull
        String analysisLayer,

        @JsonProperty("client_message")
        @NonNull
        String clientMessage,

        @JsonProperty("support_response")
        @NonNull
        String supportResponse,

        @JsonProperty("expected_metrics")
        @NonNull
        Metrics expectedMetrics,

        @NonNull
        String recommendation) {
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
