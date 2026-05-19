package com.inboxai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VoyageResponse(
        String object,
        List<Datum> data,
        String model,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Datum(String object, List<Float> embedding, int index) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(@JsonProperty("total_tokens") int totalTokens) {
    }
}
