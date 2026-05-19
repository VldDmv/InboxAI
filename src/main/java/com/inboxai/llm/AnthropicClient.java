package com.inboxai.llm;

import com.inboxai.llm.dto.AnthropicRequest;
import com.inboxai.llm.dto.AnthropicResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    private final AnthropicProperties props;
    private final RestClient restClient;

    public AnthropicClient(AnthropicProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        rf.setReadTimeout((int) props.getReadTimeout().toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(rf)
                .defaultHeader("x-api-key", props.getApiKey())
                .defaultHeader("anthropic-version", props.getApiVersion())
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public AnthropicResponse send(AnthropicRequest request) {
        long backoffMs = props.getRetryInitialBackoff().toMillis();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
            try {
                return restClient.post()
                        .uri("/v1/messages")
                        .body(request)
                        .retrieve()
                        .body(AnthropicResponse.class);
            } catch (RestClientResponseException e) {
                if (!isRetryable(e.getStatusCode())) {
                    throw e;
                }
                last = e;
                log.warn("Anthropic returned {} on attempt {}/{}: {}",
                        e.getStatusCode(), attempt, props.getMaxRetries(), e.getMessage());
            } catch (ResourceAccessException e) {
                last = e;
                log.warn("Anthropic network error on attempt {}/{}: {}",
                        attempt, props.getMaxRetries(), e.getMessage());
            }
            if (attempt < props.getMaxRetries()) {
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        throw last;
    }

    private static boolean isRetryable(HttpStatusCode status) {
        int code = status.value();
        return code == 429 || code == 529 || (code >= 500 && code < 600);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry backoff", ie);
        }
    }
}
