package com.inboxai.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicResponse(
        String id,
        String type,
        String role,
        String model,
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(
            String type,
            String text,
            String id,
            String name,
            Map<String, Object> input
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("cache_creation_input_tokens") Integer cacheCreationInputTokens,
            @JsonProperty("cache_read_input_tokens") Integer cacheReadInputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {
    }
}
