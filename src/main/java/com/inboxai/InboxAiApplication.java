package com.inboxai;

import com.inboxai.llm.AnthropicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AnthropicProperties.class)
public class InboxAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InboxAiApplication.class, args);
    }
}
