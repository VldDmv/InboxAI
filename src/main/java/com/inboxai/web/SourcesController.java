package com.inboxai.web;

import com.inboxai.domain.Source;
import com.inboxai.domain.SourceType;
import com.inboxai.domain.User;
import com.inboxai.fetch.RssFetchService;
import com.inboxai.repository.SourceRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sources")
public class SourcesController {

    private final SourceRepository sourceRepository;
    private final RssFetchService rssFetchService;
    private final CurrentUserProvider currentUser;

    public SourcesController(SourceRepository sourceRepository,
                             RssFetchService rssFetchService,
                             CurrentUserProvider currentUser) {
        this.sourceRepository = sourceRepository;
        this.rssFetchService = rssFetchService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUser.get();
        model.addAttribute("sources", sourceRepository.findByUserId(user.getId()));
        if (!model.containsAttribute("sourceForm")) {
            model.addAttribute("sourceForm", new SourceForm());
        }
        return "sources";
    }

    @PostMapping
    public String add(@Valid @ModelAttribute("sourceForm") SourceForm form,
                      BindingResult binding,
                      Model model) {
        if (binding.hasErrors()) {
            return list(model);
        }
        try {
            rssFetchService.fetchFromUrl(form.getUrl());
        } catch (Exception e) {
            binding.rejectValue("url", "feed.unreachable",
                    "Could not fetch or parse this feed: " + e.getMessage());
            return list(model);
        }

        User user = currentUser.get();
        String title = form.getTitle() == null || form.getTitle().isBlank() ? form.getUrl() : form.getTitle();
        sourceRepository.save(new Source(user, SourceType.RSS, form.getUrl(), title));
        return "redirect:/sources";
    }
}
