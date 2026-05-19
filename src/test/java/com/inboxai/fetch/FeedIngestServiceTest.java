package com.inboxai.fetch;

import com.inboxai.domain.Source;
import com.inboxai.domain.SourceType;
import com.inboxai.domain.User;
import com.inboxai.repository.ItemRepository;
import com.inboxai.repository.SourceRepository;
import com.inboxai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(FeedIngestService.class)
class FeedIngestServiceTest {

    @MockBean
    private RssFetchService rssFetchService;

    @Autowired
    private FeedIngestService feedIngestService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SourceRepository sourceRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void insertsNewItemsSkipsDuplicatesAndUpdatesLastFetchedAt() throws Exception {
        Source source = persistSource();
        FetchedItem one = new FetchedItem("g1", "T1", "https://l/1", "<p>1</p>", "1",
                Instant.parse("2026-01-01T00:00:00Z"));
        FetchedItem two = new FetchedItem("g2", "T2", "https://l/2", null, null, null);

        when(rssFetchService.fetchFromUrl(eq(source.getUrl())))
                .thenReturn(List.of(one, two));

        FeedIngestService.IngestResult first = feedIngestService.ingest(source);

        assertThat(first.fetched()).isEqualTo(2);
        assertThat(first.created()).isEqualTo(2);
        assertThat(first.error()).isNull();
        assertThat(sourceRepository.findById(source.getId()))
                .get().extracting(Source::getLastFetchedAt).isNotNull();

        FetchedItem three = new FetchedItem("g3", "T3", "https://l/3", null, null, null);
        when(rssFetchService.fetchFromUrl(eq(source.getUrl())))
                .thenReturn(List.of(one, two, three));

        FeedIngestService.IngestResult second = feedIngestService.ingest(source);

        assertThat(second.fetched()).isEqualTo(3);
        assertThat(second.created()).isEqualTo(1);
        assertThat(itemRepository.count()).isEqualTo(3);
    }

    @Test
    void returnsErrorResultWhenFetchFails() throws Exception {
        Source source = persistSource();
        when(rssFetchService.fetchFromUrl(eq(source.getUrl())))
                .thenThrow(new RuntimeException("boom"));

        FeedIngestService.IngestResult result = feedIngestService.ingest(source);

        assertThat(result.fetched()).isZero();
        assertThat(result.created()).isZero();
        assertThat(result.error()).contains("boom");
        assertThat(itemRepository.count()).isZero();
    }

    private Source persistSource() {
        User user = userRepository.save(new User("u@example.com", "hash"));
        return sourceRepository.save(new Source(user, SourceType.RSS, "https://feed.example.com/rss", "Feed"));
    }
}
