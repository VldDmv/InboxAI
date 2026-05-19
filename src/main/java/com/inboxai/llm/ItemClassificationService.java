package com.inboxai.llm;

import com.inboxai.domain.Item;
import com.inboxai.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ItemClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ItemClassificationService.class);

    private final ClassifyService classifyService;
    private final ItemRepository itemRepository;

    public ItemClassificationService(ClassifyService classifyService, ItemRepository itemRepository) {
        this.classifyService = classifyService;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void classify(Item item) {
        String body = item.getContentText() != null ? item.getContentText() : item.getContentHtml();
        ClassificationResult result = classifyService.classify(item.getTitle(), body);

        item.setCategory(result.category());
        item.setSummary(result.summary());
        item.setClassifiedAt(Instant.now());
        item.setClassificationCostUsd(result.costUsd());
        itemRepository.save(item);

        log.info("Classified item {}: category={} cost=${} tokens(in={}/cache_read={}/cache_create={}/out={})",
                item.getId(), result.category(), result.costUsd(),
                result.inputTokens(), result.cacheReadTokens(),
                result.cacheCreationTokens(), result.outputTokens());
    }
}
