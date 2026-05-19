package com.inboxai.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmbeddingScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingScheduler.class);

    private final ItemEmbeddingDao dao;
    private final ItemEmbeddingService embeddingService;
    private final int batchSize;

    public EmbeddingScheduler(ItemEmbeddingDao dao,
                              ItemEmbeddingService embeddingService,
                              @Value("${inboxai.embed.batch-size:20}") int batchSize) {
        this.dao = dao;
        this.embeddingService = embeddingService;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${inboxai.embed.interval-ms:60000}",
            initialDelayString = "${inboxai.embed.initial-delay-ms:20000}"
    )
    public void embedBatch() {
        List<UnembeddedItem> pending = dao.findUnembedded(batchSize);
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Embedding batch of {} items", pending.size());
        for (UnembeddedItem item : pending) {
            try {
                embeddingService.embed(item);
            } catch (Exception e) {
                log.warn("Failed to embed item {}: {}", item.id(), e.toString());
            }
        }
    }
}
