package buzmakov.Prototype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record Recommendation(

    @NonNull
    @JsonProperty("title")
    String title,

    @NonNull
    @JsonProperty("triggers")
    String triggers,

    @NonNull
    @JsonProperty("advice")
    String advice
) {}
