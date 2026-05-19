package com.inboxai.web;

import com.inboxai.domain.Category;
import com.inboxai.domain.Item;
import com.inboxai.domain.User;
import com.inboxai.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InboxController {

    private static final int PAGE_SIZE = 20;

    private final ItemRepository itemRepository;
    private final CurrentUserProvider currentUser;

    public InboxController(ItemRepository itemRepository, CurrentUserProvider currentUser) {
        this.itemRepository = itemRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String inbox(@RequestParam(required = false) Category category,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        User user = currentUser.get();
        Page<Item> items = itemRepository.findFeed(
                user.getId(), category, PageRequest.of(Math.max(0, page), PAGE_SIZE));

        model.addAttribute("items", items.getContent());
        model.addAttribute("page", items);
        model.addAttribute("activeCategory", category);
        model.addAttribute("categories", Category.values());
        return "inbox";
    }
}
