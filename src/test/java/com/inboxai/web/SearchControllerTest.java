package com.inboxai.web;

import com.inboxai.embedding.SimilarItem;
import com.inboxai.embedding.SimilaritySearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SearchController.class)
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimilaritySearchService searchService;

    @Test
    void getWithoutQueryRendersEmptyForm() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attribute("results", List.of()))
                .andExpect(model().attribute("query", ""));

        verify(searchService, never()).searchByText(any(), anyInt());
    }

    @Test
    void getWithQueryInvokesSearchAndRendersResults() throws Exception {
        when(searchService.searchByText(eq("pgvector"), eq(20)))
                .thenReturn(List.of(
                        new SimilarItem(1L, "pgvector intro", "Using pgvector with Postgres", "https://l/1", 0.12)
                ));

        mockMvc.perform(get("/search").param("q", "pgvector"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("query", "pgvector"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pgvector intro")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0.1200")));
    }

    @Test
    void getWithFailingSearchShowsError() throws Exception {
        when(searchService.searchByText(any(), anyInt())).thenThrow(new RuntimeException("api down"));

        mockMvc.perform(get("/search").param("q", "anything"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Search failed")));
    }
}
