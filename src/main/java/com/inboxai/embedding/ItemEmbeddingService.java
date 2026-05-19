package com.inboxai.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ItemEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ItemEmbeddingService.class);
    private static final int MAX_CHARS = 8000;

    private final VoyageEmbeddingClient client;
    private final ItemEmbeddingDao dao;

    public ItemEmbeddingService(VoyageEmbeddingClient client, ItemEmbeddingDao dao) {
        this.client = client;
        this.dao = dao;
    }

    public void embed(UnembeddedItem item) {
        String text = buildText(item.title(), item.contentText());
        if (text.isBlank()) {
            log.debug("Skipping item {}: no text to embed", item.id());
            return;
        }
        float[] vector = client.embed(text, VoyageEmbeddingClient.InputType.DOCUMENT);
        dao.saveEmbedding(item.id(), vector);
        log.info("Embedded item {} ({} dims, {} chars)", item.id(), vector.length, text.length());
    }

    static String buildText(String title, String contentText) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title.strip()).append('\n');
        }
        if (contentText != null && !contentText.isBlank()) {
            sb.append(contentText.strip());
        }
        String text = sb.toString();
        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS);
        }
        return text;
    }
}
