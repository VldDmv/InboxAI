package com.inboxai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoyageRequest(
        List<String> input,
        String model,
        @JsonProperty("input_type") String inputType
) {
}
