package com.inboxai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InboxAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InboxAiApplication.class, args);
    }
}
