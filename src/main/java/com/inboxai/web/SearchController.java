package com.inboxai.web;

import com.inboxai.embedding.SimilarItem;
import com.inboxai.embedding.SimilaritySearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    private static final int RESULT_LIMIT = 20;

    private final SimilaritySearchService searchService;

    public SearchController(SimilaritySearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public String search(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("query", query == null ? "" : query);
        if (query == null || query.isBlank()) {
            model.addAttribute("results", List.of());
            return "search";
        }
        try {
            List<SimilarItem> results = searchService.searchByText(query, RESULT_LIMIT);
            model.addAttribute("results", results);
        } catch (Exception e) {
            log.warn("Search failed for query '{}': {}", query, e.toString());
            model.addAttribute("results", List.of());
            model.addAttribute("error", "Search failed: " + e.getMessage());
        }
        return "search";
    }
}
