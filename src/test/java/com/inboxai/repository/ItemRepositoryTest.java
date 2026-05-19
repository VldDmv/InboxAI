package com.inboxai.repository;

import com.inboxai.domain.Item;
import com.inboxai.domain.Source;
import com.inboxai.domain.SourceType;
import com.inboxai.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ItemRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void persistsAndFindsItemByGuid() {
        Source source = persistSource();

        Item item = new Item(source, "guid-1", "Hello", "https://example.com/1",
                "<p>html</p>", "html", Instant.parse("2026-01-01T00:00:00Z"));
        itemRepository.save(item);

        assertThat(itemRepository.findBySourceIdAndGuid(source.getId(), "guid-1"))
                .isPresent()
                .get()
                .extracting(Item::getTitle)
                .isEqualTo("Hello");
    }

    @Test
    void rejectsDuplicateGuidWithinSource() {
        Source source = persistSource();
        itemRepository.saveAndFlush(new Item(source, "dup", "A", null, null, null, null));

        assertThatThrownBy(() ->
                itemRepository.saveAndFlush(new Item(source, "dup", "B", null, null, null, null))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Source persistSource() {
        User user = userRepository.save(new User("u@example.com", "hash"));
        return sourceRepository.save(new Source(user, SourceType.RSS, "https://feed", "Feed"));
    }
}
