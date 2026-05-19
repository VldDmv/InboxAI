package com.inboxai.web;

import com.inboxai.domain.Category;
import com.inboxai.domain.Item;
import com.inboxai.domain.Source;
import com.inboxai.domain.SourceType;
import com.inboxai.domain.User;
import com.inboxai.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(InboxController.class)
@ActiveProfiles("test")
class InboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private CurrentUserProvider currentUser;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = new User("u@example.com", "");
        setId(user, "id", 7L);
        when(currentUser.get()).thenReturn(user);
    }

    @Test
    void rendersInboxWithItemsAndCategoryChips() throws Exception {
        Source source = new Source(user, SourceType.RSS, "https://feed", "Feed One");
        setId(source, "id", 1L);
        Item item = new Item(source, "g1", "Spring 3.4 released", "https://link/1",
                null, "body", Instant.parse("2026-02-01T00:00:00Z"));
        item.setCategory(Category.NEWS);
        item.setSummary("Spring 3.4 shipped with virtual threads.");
        setId(item, "id", 11L);

        when(itemRepository.findFeed(eq(7L), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inbox"))
                .andExpect(model().attributeExists("items", "page", "categories"))
                .andExpect(model().attribute("activeCategory", (Category) null))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring 3.4 released")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Feed One")));
    }

    @Test
    void appliesCategoryFilterFromQueryParam() throws Exception {
        when(itemRepository.findFeed(eq(7L), eq(Category.IMPORTANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/").param("category", "IMPORTANT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeCategory", Category.IMPORTANT));
    }

    @Test
    void showsEmptyStateWhenNoItems() throws Exception {
        when(itemRepository.findFeed(eq(7L), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No items yet")));
    }

    private static void setId(Object target, String fieldName, Long value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
