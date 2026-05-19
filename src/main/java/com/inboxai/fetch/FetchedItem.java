package com.inboxai.fetch;

import java.time.Instant;

public record FetchedItem(
        String guid,
        String title,
        String link,
        String contentHtml,
        String contentText,
        Instant publishedAt
) {
}
