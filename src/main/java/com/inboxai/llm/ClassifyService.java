package com.inboxai.llm;

import com.inboxai.domain.Category;
import com.inboxai.llm.dto.AnthropicRequest;
import com.inboxai.llm.dto.AnthropicResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClassifyService {

    private static final String TOOL_NAME = "classify_item";
    private static final int MAX_CONTENT_CHARS = 4000;

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "category", Map.of(
                            "type", "string",
                            "enum", List.of("news", "important", "promo", "reply_needed", "other"),
                            "description", "Single category for this item."
                    ),
                    "summary", Map.of(
                            "type", "string",
                            "description", "One-sentence concrete summary, 80–150 chars typical."
                    ),
                    "confidence", Map.of(
                            "type", "number",
                            // strict mode rejects minimum/maximum constraints,
                            // so the range lives in the description only
                            "description", "Confidence in the classification, 0.0–1.0."
                    )
            ),
            "required", List.of("category", "summary", "confidence"),
            "additionalProperties", false
    );

    // strict: true makes the API guarantee the tool input validates against
    // the schema, so parse() can't see a missing field or unknown category
    private static final AnthropicRequest.Tool CLASSIFY_TOOL = new AnthropicRequest.Tool(
            TOOL_NAME,
            "Classify an inbox item and produce a one-sentence summary.",
            INPUT_SCHEMA,
            true
    );

    private final AnthropicClient client;
    private final AnthropicProperties props;
    private final CostCalculator costCalculator;
    private final String systemPrompt;

    public ClassifyService(AnthropicClient client,
                           AnthropicProperties props,
                           CostCalculator costCalculator,
                           @Value("classpath:prompts/classify-system.txt") Resource systemPromptResource) throws IOException {
        this.client = client;
        this.props = props;
        this.costCalculator = costCalculator;
        this.systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
    }

    public ClassificationResult classify(String title, String content) {
        AnthropicRequest request = new AnthropicRequest(
                props.getModel(),
                props.getMaxTokens(),
                List.of(AnthropicRequest.SystemBlock.cached(systemPrompt)),
                List.of(CLASSIFY_TOOL),
                AnthropicRequest.ToolChoice.tool(TOOL_NAME),
                List.of(AnthropicRequest.Message.user(buildUserMessage(title, content)))
        );
        AnthropicResponse response = client.send(request);
        return parse(response);
    }

    private static String buildUserMessage(String title, String content) {
        String safeTitle = title == null ? "" : title.strip();
        String safeContent = content == null ? "" : content.strip();
        if (safeContent.length() > MAX_CONTENT_CHARS) {
            safeContent = safeContent.substring(0, MAX_CONTENT_CHARS) + "…";
        }
        return "Title: " + safeTitle + "\n\nBody:\n" + safeContent;
    }

    private ClassificationResult parse(AnthropicResponse response) {
        if (response == null || response.content() == null) {
            throw new ClassificationException("Empty Anthropic response");
        }
        AnthropicResponse.ContentBlock toolUse = response.content().stream()
                .filter(b -> "tool_use".equals(b.type()) && TOOL_NAME.equals(b.name()))
                .findFirst()
                .orElseThrow(() -> new ClassificationException(
                        "No tool_use block named " + TOOL_NAME + " in response"));

        Map<String, Object> input = Objects.requireNonNullElse(toolUse.input(), Map.of());
        Category category = parseCategory(asString(input.get("category")));
        String summary = asString(input.get("summary"));
        double confidence = asDouble(input.get("confidence"));

        AnthropicResponse.Usage usage = Objects.requireNonNullElse(
                response.usage(),
                new AnthropicResponse.Usage(0, null, null, 0));

        return new ClassificationResult(
                category,
                summary,
                confidence,
                costCalculator.compute(response.model(), usage),
                usage.inputTokens(),
                usage.cacheCreationInputTokens() == null ? 0 : usage.cacheCreationInputTokens(),
                usage.cacheReadInputTokens() == null ? 0 : usage.cacheReadInputTokens(),
                usage.outputTokens()
        );
    }

    private static Category parseCategory(String raw) {
        if (raw == null) {
            throw new ClassificationException("Missing 'category' in tool_use input");
        }
        try {
            return Category.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClassificationException("Unknown category: " + raw);
        }
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static double asDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static class ClassificationException extends RuntimeException {
        public ClassificationException(String message) {
            super(message);
        }
    }
}
