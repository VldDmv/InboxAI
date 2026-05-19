package com.inboxai.llm;

import com.inboxai.domain.Category;

import java.math.BigDecimal;

public record ClassificationResult(
        Category category,
        String summary,
        double confidence,
        BigDecimal costUsd,
        int inputTokens,
        int cacheCreationTokens,
        int cacheReadTokens,
        int outputTokens
) {
}
