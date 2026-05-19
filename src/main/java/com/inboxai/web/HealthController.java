package com.inboxai.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "app", "InboxAI",
                "status", "up",
                "version", "0.1.0-SNAPSHOT"
        );
    }
}
