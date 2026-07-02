package com.inboxai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.inboxai.domain.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassifyServiceTest {

    private static final String SYSTEM_PROMPT = "INBOXAI SYSTEM PROMPT v1 — categorise items into news/important/promo/reply_needed/other";

    private WireMockServer wireMock;
    private ClassifyService service;
    private AnthropicProperties props;

    @BeforeEach
    void setUp() throws IOException {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        props = new AnthropicProperties();
        props.setApiKey("test-key");
        props.setBaseUrl("http://localhost:" + wireMock.port());
        props.setModel("claude-haiku-4-5-20251001");
        props.setMaxTokens(256);
        props.setMaxRetries(3);
        props.setRetryInitialBackoff(java.time.Duration.ofMillis(5));

        service = new ClassifyService(
                new AnthropicClient(props),
                props,
                new CostCalculator(),
                new ByteArrayResource(SYSTEM_PROMPT.getBytes())
        );
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void buildsRequestWithCacheControlOnSystemAndForcedTool() throws Exception {
        stubAnthropic(toolUseResponse("news", "Test news summary.", 0.92,
                500, 1200, 0, 60));

        service.classify("Hello", "Some body content");

        LoggedRequest req = wireMock.findAll(postRequestedFor(urlEqualTo("/v1/messages"))).get(0);
        assertThat(req.header("x-api-key").firstValue()).isEqualTo("test-key");
        assertThat(req.header("anthropic-version").firstValue()).isEqualTo("2023-06-01");

        JsonNode body = new ObjectMapper().readTree(req.getBody());
        assertThat(body.get("model").asText()).isEqualTo("claude-haiku-4-5-20251001");
        assertThat(body.get("max_tokens").asInt()).isEqualTo(256);

        JsonNode systemBlock = body.get("system").get(0);
        assertThat(systemBlock.get("type").asText()).isEqualTo("text");
        assertThat(systemBlock.get("text").asText()).contains("INBOXAI SYSTEM PROMPT");
        assertThat(systemBlock.get("cache_control").get("type").asText()).isEqualTo("ephemeral");

        assertThat(body.get("tool_choice").get("type").asText()).isEqualTo("tool");
        assertThat(body.get("tool_choice").get("name").asText()).isEqualTo("classify_item");
        assertThat(body.get("tools").get(0).get("name").asText()).isEqualTo("classify_item");
        assertThat(body.get("tools").get(0).get("strict").asBoolean()).isTrue();
        assertThat(body.get("tools").get(0).get("input_schema").get("additionalProperties").asBoolean()).isFalse();

        assertThat(body.get("messages").get(0).get("role").asText()).isEqualTo("user");
        assertThat(body.get("messages").get(0).get("content").asText())
                .contains("Title: Hello")
                .contains("Some body content");
    }

    @Test
    void parsesToolUseResponseIntoClassificationResult() {
        stubAnthropic(toolUseResponse("important", "Critical CVE patched in 6.1.8.", 0.97,
                500, 0, 1200, 80));

        ClassificationResult result = service.classify("CVE-2025-1234", "RCE in Spring Framework.");

        assertThat(result.category()).isEqualTo(Category.IMPORTANT);
        assertThat(result.summary()).isEqualTo("Critical CVE patched in 6.1.8.");
        assertThat(result.confidence()).isEqualTo(0.97);
        assertThat(result.inputTokens()).isEqualTo(500);
        assertThat(result.cacheReadTokens()).isEqualTo(1200);
        assertThat(result.cacheCreationTokens()).isZero();
        assertThat(result.outputTokens()).isEqualTo(80);
        // cache-hit cost: 500*1/1e6 + 1200*0.10/1e6 + 80*5/1e6
        //               = 0.0005 + 0.00012 + 0.0004 = 0.00102
        assertThat(result.costUsd()).isEqualByComparingTo("0.00102000");
    }

    @Test
    void retriesOn529ThenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .inScenario("retry").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(529).withBody("{\"type\":\"overloaded_error\"}"))
                .willSetStateTo("attempt-2"));
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .inScenario("retry").whenScenarioStateIs("attempt-2")
                .willReturn(aResponse().withStatus(200).withHeader("content-type", "application/json")
                        .withBody(toolUseResponse("news", "OK.", 0.8, 10, 0, 0, 20))));

        ClassificationResult result = service.classify("t", "b");

        assertThat(result.category()).isEqualTo(Category.NEWS);
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/v1/messages")))).hasSize(2);
    }

    @Test
    void doesNotRetryOn400() {
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("content-type", "application/json")
                        .withBody("{\"type\":\"invalid_request_error\",\"message\":\"bad\"}")));

        assertThatThrownBy(() -> service.classify("t", "b"))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/v1/messages")))).hasSize(1);
    }

    @Test
    void throwsWhenResponseHasNoToolUse() {
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("content-type", "application/json")
                        .withBody("""
                                {
                                  "id":"m1","type":"message","role":"assistant",
                                  "model":"claude-haiku-4-5-20251001",
                                  "content":[{"type":"text","text":"hi"}],
                                  "stop_reason":"end_turn",
                                  "usage":{"input_tokens":1,"output_tokens":1}
                                }
                                """)));

        assertThatThrownBy(() -> service.classify("t", "b"))
                .isInstanceOf(ClassifyService.ClassificationException.class)
                .hasMessageContaining("tool_use");
    }

    @Test
    void issuesIdenticalSystemBlockAcrossCallsSoAnthropicCanReuseCache() throws Exception {
        stubAnthropic(toolUseResponse("news", "s1", 0.9, 10, 1500, 0, 20));
        service.classify("first title", "first body");
        service.classify("second title", "second body");

        List<LoggedRequest> reqs = wireMock.findAll(postRequestedFor(urlEqualTo("/v1/messages")));
        assertThat(reqs).hasSize(2);
        ObjectMapper om = new ObjectMapper();
        JsonNode sys1 = om.readTree(reqs.get(0).getBody()).get("system");
        JsonNode sys2 = om.readTree(reqs.get(1).getBody()).get("system");
        assertThat(sys1).isEqualTo(sys2);
    }

    private void stubAnthropic(String responseBody) {
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .withHeader("x-api-key", equalTo("test-key"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("content-type", "application/json")
                        .withBody(responseBody)));
    }

    private static String toolUseResponse(String category, String summary, double confidence,
                                          int inputTokens, int cacheCreate, int cacheRead, int outputTokens) {
        return """
                {
                  "id":"msg_01",
                  "type":"message",
                  "role":"assistant",
                  "model":"claude-haiku-4-5-20251001",
                  "content":[{
                    "type":"tool_use",
                    "id":"tool_01",
                    "name":"classify_item",
                    "input":{"category":"%s","summary":"%s","confidence":%s}
                  }],
                  "stop_reason":"tool_use",
                  "usage":{
                    "input_tokens":%d,
                    "cache_creation_input_tokens":%d,
                    "cache_read_input_tokens":%d,
                    "output_tokens":%d
                  }
                }
                """.formatted(category, summary, confidence,
                inputTokens, cacheCreate, cacheRead, outputTokens);
    }
}
