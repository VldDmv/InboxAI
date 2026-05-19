package com.inboxai.llm;

import com.inboxai.domain.Item;
import com.inboxai.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClassificationScheduler.class);

    private final ItemRepository itemRepository;
    private final ItemClassificationService itemClassificationService;
    private final int batchSize;

    public ClassificationScheduler(ItemRepository itemRepository,
                                   ItemClassificationService itemClassificationService,
                                   @Value("${inboxai.classify.batch-size:20}") int batchSize) {
        this.itemRepository = itemRepository;
        this.itemClassificationService = itemClassificationService;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${inboxai.classify.interval-ms:60000}",
            initialDelayString = "${inboxai.classify.initial-delay-ms:15000}"
    )
    public void classifyBatch() {
        List<Item> pending = itemRepository.findByClassifiedAtIsNullOrderByFetchedAtAsc(
                PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Classifying batch of {} items", pending.size());
        for (Item item : pending) {
            try {
                itemClassificationService.classify(item);
            } catch (Exception e) {
                log.warn("Failed to classify item {}: {}", item.getId(), e.toString());
            }
        }
    }
}
