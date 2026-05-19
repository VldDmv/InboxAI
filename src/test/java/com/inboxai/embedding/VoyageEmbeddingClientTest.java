package com.inboxai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoyageEmbeddingClientTest {

    private WireMockServer wireMock;
    private VoyageEmbeddingClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        VoyageProperties props = new VoyageProperties();
        props.setApiKey("vk-test");
        props.setBaseUrl("http://localhost:" + wireMock.port());
        props.setModel("voyage-3-lite");
        props.setDimension(4);
        props.setMaxRetries(3);
        props.setRetryInitialBackoff(Duration.ofMillis(5));

        client = new VoyageEmbeddingClient(props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void sendsBearerAuthAndCorrectBody() throws Exception {
        stubOk(embeddingResponse(0.1f, 0.2f, 0.3f, 0.4f));

        client.embed("hello world", VoyageEmbeddingClient.InputType.DOCUMENT);

        LoggedRequest req = wireMock.findAll(postRequestedFor(urlEqualTo("/v1/embeddings"))).get(0);
        assertThat(req.header("Authorization").firstValue()).isEqualTo("Bearer vk-test");

        JsonNode body = new ObjectMapper().readTree(req.getBody());
        assertThat(body.get("model").asText()).isEqualTo("voyage-3-lite");
        assertThat(body.get("input_type").asText()).isEqualTo("document");
        assertThat(body.get("input").isArray()).isTrue();
        assertThat(body.get("input").get(0).asText()).isEqualTo("hello world");
    }

    @Test
    void sendsQueryInputTypeForSearch() throws Exception {
        stubOk(embeddingResponse(0.1f, 0.2f, 0.3f, 0.4f));

        client.embed("find me docs", VoyageEmbeddingClient.InputType.QUERY);

        LoggedRequest req = wireMock.findAll(postRequestedFor(urlEqualTo("/v1/embeddings"))).get(0);
        JsonNode body = new ObjectMapper().readTree(req.getBody());
        assertThat(body.get("input_type").asText()).isEqualTo("query");
    }

    @Test
    void parsesEmbeddingIntoFloatArray() {
        stubOk(embeddingResponse(0.1f, 0.2f, 0.3f, 0.4f));

        float[] vec = client.embed("x", VoyageEmbeddingClient.InputType.DOCUMENT);

        assertThat(vec).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void rejectsMismatchedDimension() {
        stubOk(embeddingResponse(0.1f, 0.2f));

        assertThatThrownBy(() -> client.embed("x", VoyageEmbeddingClient.InputType.DOCUMENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected 4")
                .hasMessageContaining("got 2");
    }

    @Test
    void retriesOn503ThenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
                .inScenario("retry").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503).withBody("{\"detail\":\"upstream\"}"))
                .willSetStateTo("attempt-2"));
        wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
                .inScenario("retry").whenScenarioStateIs("attempt-2")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("content-type", "application/json")
                        .withBody(embeddingResponse(0.0f, 0.0f, 0.0f, 0.0f))));

        client.embed("x", VoyageEmbeddingClient.InputType.DOCUMENT);

        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/v1/embeddings")))).hasSize(2);
    }

    @Test
    void doesNotRetryOn401() {
        wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse().withStatus(401)
                        .withHeader("content-type", "application/json")
                        .withBody("{\"detail\":\"invalid key\"}")));

        assertThatThrownBy(() -> client.embed("x", VoyageEmbeddingClient.InputType.DOCUMENT))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/v1/embeddings")))).hasSize(1);
    }

    private void stubOk(String body) {
        wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
                .withHeader("Authorization", equalTo("Bearer vk-test"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("content-type", "application/json")
                        .withBody(body)));
    }

    private static String embeddingResponse(float... values) {
        String arr = IntStream.range(0, values.length)
                .mapToObj(i -> Float.toString(values[i]))
                .collect(Collectors.joining(","));
        return """
                {
                  "object":"list",
                  "data":[{"object":"embedding","embedding":[%s],"index":0}],
                  "model":"voyage-3-lite",
                  "usage":{"total_tokens":12}
                }
                """.formatted(arr);
    }
}
