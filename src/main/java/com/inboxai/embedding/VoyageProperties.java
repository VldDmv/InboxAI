package com.inboxai.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "inboxai.voyage")
public class VoyageProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.voyageai.com";
    private String model = "voyage-3-lite";
    private int dimension = 512;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(60);
    private int maxRetries = 3;
    private Duration retryInitialBackoff = Duration.ofSeconds(1);

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getDimension() { return dimension; }
    public void setDimension(int dimension) { this.dimension = dimension; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public Duration getRetryInitialBackoff() { return retryInitialBackoff; }
    public void setRetryInitialBackoff(Duration retryInitialBackoff) { this.retryInitialBackoff = retryInitialBackoff; }
}
