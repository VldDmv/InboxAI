package com.inboxai.fetch;

import com.inboxai.domain.Item;
import com.inboxai.domain.Source;
import com.inboxai.repository.ItemRepository;
import com.inboxai.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FeedIngestService {

    private static final Logger log = LoggerFactory.getLogger(FeedIngestService.class);

    private final RssFetchService rssFetchService;
    private final SourceRepository sourceRepository;
    private final ItemRepository itemRepository;

    public FeedIngestService(RssFetchService rssFetchService,
                             SourceRepository sourceRepository,
                             ItemRepository itemRepository) {
        this.rssFetchService = rssFetchService;
        this.sourceRepository = sourceRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public IngestResult ingest(Source source) {
        List<FetchedItem> fetched;
        try {
            fetched = rssFetchService.fetchFromUrl(source.getUrl());
        } catch (Exception e) {
            log.warn("Failed to fetch source {} ({}): {}", source.getId(), source.getUrl(), e.toString());
            return new IngestResult(0, 0, e.toString());
        }

        int created = 0;
        for (FetchedItem f : fetched) {
            if (itemRepository.existsBySourceIdAndGuid(source.getId(), f.guid())) {
                continue;
            }
            Item item = new Item(source, f.guid(), f.title(), f.link(),
                    f.contentHtml(), f.contentText(), f.publishedAt());
            itemRepository.save(item);
            created++;
        }

        source.setLastFetchedAt(Instant.now());
        sourceRepository.save(source);
        log.info("Source {} ({}): fetched={} new={}", source.getId(), source.getUrl(), fetched.size(), created);
        return new IngestResult(fetched.size(), created, null);
    }

    public record IngestResult(int fetched, int created, String error) {
    }
}
