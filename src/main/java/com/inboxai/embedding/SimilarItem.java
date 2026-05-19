package com.inboxai.embedding;

public record SimilarItem(
        long id,
        String title,
        String summary,
        String link,
        double distance
) {
}
