package io.dqengine.example.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dqengine.testing.FixtureRecords;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Filters, request errors (Spring's problem responses) and a failing rules service could get their own tests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.banner-mode=off", "logging.level.root=WARN"})
class ValidationApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // not reset between tests: the rules are fetched once, when the app starts
    @RegisterExtension
    static final WireMockExtension RULES_SERVICE = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .resetOnEachTest(false)
            .build();

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void rulesService(DynamicPropertyRegistry properties) {
        properties.add("dq.rules-url", () -> RULES_SERVICE.baseUrl() + "/v1/rules");
    }

    @BeforeAll
    static void serveRules() throws IOException {
        RULES_SERVICE.stubFor(get("/v1/rules").willReturn(okJson(resource("/rules.json"))));
    }

    @Test
    void fixtureIsValidatedWithTheRulesFromTheRulesService() throws Exception {
        HttpResponse<String> response = post(resource(FixtureRecords.JSON_RESOURCE));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("summary")).isEqualTo(JSON.readTree("""
                { "records": 3, "results": 9, "errors": 0,
                  "byDecision": {"VALID": 3, "INVALID": 4, "REVIEW": 0, "NOT_APPLICABLE": 2},
                  "bySeverity": {"ERROR": 6, "WARNING": 3, "INFO": 0} }"""));
        assertThat(body.get("outcomes").get(2).get("results").get(2)).isEqualTo(JSON.readTree("""
                { "decision": "INVALID", "severity": "WARNING",
                  "provenance": { "ruleId": "ibanFormat", "recordId": "r3",
                                  "fieldsRead": [ {"field": "iban", "value": ""}, {"field": "country", "value": "ZZ"} ],
                                  "computedValue": "bad" } }"""));
        RULES_SERVICE.verify(1, getRequestedFor(urlEqualTo("/v1/rules")));
    }

    @Test
    void failingRuleAndBadRecordAreReportedAndTheRestIsValidated() throws Exception {
        HttpResponse<String> response = post("""
                [ {"id": "r4", "vatId": "DE444444444", "country": "DE", "iban": 12345},
                  {"legalName": "no id"} ]""");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("outcomes").get(0).get("results")).hasSize(2);
        assertThat(body.get("outcomes").get(0).get("errors").get(0).get("ruleId").asString()).isEqualTo("ibanFormat");
        assertThat(body.get("outcomes").get(1).get("errors").get(0).get("type").asString()).isEqualTo("RECORD");
        assertThat(body.get("summary").get("errors").asInt()).isEqualTo(2);
    }

    @Test
    void requestOverTheRecordLimitIsRejected() throws Exception {
        HttpResponse<String> response = post("[" + "{\"id\": \"r\"},".repeat(ValidationController.MAX_RECORDS)
                + "{\"id\": \"r\"}]");

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(JSON.readTree(response.body()).get("detail").asString())
                .isEqualTo("at most 1000 records per request");
    }

    private HttpResponse<String> post(String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/validations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = ValidationApiTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), UTF_8);
        }
    }
}
