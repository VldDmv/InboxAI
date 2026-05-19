package com.inboxai.web;

import com.inboxai.domain.User;
import com.inboxai.fetch.FetchedItem;
import com.inboxai.fetch.RssFetchService;
import com.inboxai.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SourcesController.class)
@ActiveProfiles("test")
class SourcesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SourceRepository sourceRepository;
    @MockBean
    private RssFetchService rssFetchService;
    @MockBean
    private CurrentUserProvider currentUser;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = new User("u@example.com", "");
        setId(user, "id", 3L);
        when(currentUser.get()).thenReturn(user);
        when(sourceRepository.findByUserId(3L)).thenReturn(List.of());
    }

    @Test
    @WithMockUser(username = "u@example.com")
    void getRendersFormAndEmptyList() throws Exception {
        mockMvc.perform(get("/sources"))
                .andExpect(status().isOk())
                .andExpect(view().name("sources"))
                .andExpect(model().attributeExists("sourceForm", "sources"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No sources yet")));
    }

    @Test
    @WithMockUser(username = "u@example.com")
    void postWithValidUrlFetchesAndPersists() throws Exception {
        when(rssFetchService.fetchFromUrl("https://feed.example.com/rss"))
                .thenReturn(List.of(new FetchedItem("g1", "t", "l", null, null, null)));

        mockMvc.perform(post("/sources").with(csrf())
                        .param("url", "https://feed.example.com/rss")
                        .param("title", "My Feed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sources"));

        verify(sourceRepository).save(any());
    }

    @Test
    @WithMockUser(username = "u@example.com")
    void postWithUnreachableFeedShowsErrorAndDoesNotSave() throws Exception {
        when(rssFetchService.fetchFromUrl(any())).thenThrow(new RuntimeException("404"));

        mockMvc.perform(post("/sources").with(csrf())
                        .param("url", "https://broken.example.com/rss"))
                .andExpect(status().isOk())
                .andExpect(view().name("sources"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Could not fetch")));

        verify(sourceRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "u@example.com")
    void postWithBlankUrlShowsValidationError() throws Exception {
        mockMvc.perform(post("/sources").with(csrf())
                        .param("url", "")
                        .param("title", "x"))
                .andExpect(status().isOk())
                .andExpect(view().name("sources"));

        verify(rssFetchService, never()).fetchFromUrl(any());
        verify(sourceRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "u@example.com")
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/sources")
                        .param("url", "https://example.com/rss"))
                .andExpect(status().isForbidden());

        verify(sourceRepository, never()).save(any());
    }

    private static void setId(Object target, String fieldName, Long value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
