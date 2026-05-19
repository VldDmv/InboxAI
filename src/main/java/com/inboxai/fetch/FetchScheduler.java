package com.inboxai.fetch;

import com.inboxai.domain.Source;
import com.inboxai.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(FetchScheduler.class);

    private final SourceRepository sourceRepository;
    private final FeedIngestService feedIngestService;

    public FetchScheduler(SourceRepository sourceRepository, FeedIngestService feedIngestService) {
        this.sourceRepository = sourceRepository;
        this.feedIngestService = feedIngestService;
    }

    @Scheduled(
            fixedDelayString = "${inboxai.fetch.interval-ms:600000}",
            initialDelayString = "${inboxai.fetch.initial-delay-ms:10000}"
    )
    public void pollAll() {
        List<Source> sources = sourceRepository.findByActiveTrue();
        if (sources.isEmpty()) {
            return;
        }
        log.debug("Polling {} active sources", sources.size());
        for (Source source : sources) {
            feedIngestService.ingest(source);
        }
    }
}
