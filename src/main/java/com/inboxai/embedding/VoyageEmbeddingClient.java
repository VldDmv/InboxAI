package com.inboxai.embedding;

import com.inboxai.embedding.dto.VoyageRequest;
import com.inboxai.embedding.dto.VoyageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class VoyageEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(VoyageEmbeddingClient.class);

    public enum InputType {
        QUERY("query"), DOCUMENT("document");

        private final String value;
        InputType(String value) { this.value = value; }
        public String value() { return value; }
    }

    private final VoyageProperties props;
    private final RestClient restClient;

    public VoyageEmbeddingClient(VoyageProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        rf.setReadTimeout((int) props.getReadTimeout().toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(rf)
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public float[] embed(String text, InputType inputType) {
        VoyageRequest request = new VoyageRequest(List.of(text), props.getModel(), inputType.value());
        VoyageResponse response = send(request);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Empty Voyage response");
        }
        List<Float> embedding = response.data().get(0).embedding();
        if (embedding == null) {
            throw new IllegalStateException("Voyage response missing embedding array");
        }
        if (embedding.size() != props.getDimension()) {
            throw new IllegalStateException(
                    "Embedding dimension mismatch: expected " + props.getDimension()
                            + ", got " + embedding.size());
        }
        return toFloatArray(embedding);
    }

    private VoyageResponse send(VoyageRequest request) {
        long backoffMs = props.getRetryInitialBackoff().toMillis();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
            try {
                return restClient.post()
                        .uri("/v1/embeddings")
                        .body(request)
                        .retrieve()
                        .body(VoyageResponse.class);
            } catch (RestClientResponseException e) {
                if (!isRetryable(e.getStatusCode())) {
                    throw e;
                }
                last = e;
                log.warn("Voyage returned {} on attempt {}/{}: {}",
                        e.getStatusCode(), attempt, props.getMaxRetries(), e.getMessage());
            } catch (ResourceAccessException e) {
                last = e;
                log.warn("Voyage network error on attempt {}/{}: {}",
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
        return code == 429 || (code >= 500 && code < 600);
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] out = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
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
