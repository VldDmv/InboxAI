package com.inboxai.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        List<SystemBlock> system,
        List<Tool> tools,
        @JsonProperty("tool_choice") ToolChoice toolChoice,
        List<Message> messages
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemBlock(
            String type,
            String text,
            @JsonProperty("cache_control") CacheControl cacheControl
    ) {
        public static SystemBlock cached(String text) {
            return new SystemBlock("text", text, new CacheControl("ephemeral"));
        }
    }

    public record CacheControl(String type) {
    }

    public record Tool(
            String name,
            String description,
            @JsonProperty("input_schema") Map<String, Object> inputSchema,
            Boolean strict
    ) {
    }

    public record ToolChoice(String type, String name) {
        public static ToolChoice tool(String name) {
            return new ToolChoice("tool", name);
        }
    }

    public record Message(String role, String content) {
        public static Message user(String content) {
            return new Message("user", content);
        }
    }
}
