package ma.ac.university.darijatranslator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class BackendEndpointIT {
    private static final int STUB_PORT = 19090;
    private static final String BASE_URL = "http://localhost:"
            + System.getProperty("http.port", "9080") + "/api";
    private static final String TEST_USERNAME = "integration-test-user";
    private static final String TEST_PASSWORD = "integration-test-password";
    private final HttpClient client = HttpClient.newHttpClient();
    private static HttpServer providerStub;
    private static ExecutorService providerExecutor;
    private static final AtomicInteger PROVIDER_REQUEST_COUNT = new AtomicInteger();

    @BeforeAll
    static void startProviderStub() throws IOException {
        providerStub = HttpServer.create(new java.net.InetSocketAddress("localhost", STUB_PORT), 0);
        providerExecutor = Executors.newCachedThreadPool();
        providerStub.setExecutor(providerExecutor);
        providerStub.createContext("/v1beta/models/stub-model:generateContent",
                BackendEndpointIT::handleProviderRequest);
        providerStub.start();
    }

    @AfterAll
    static void stopProviderStub() {
        if (providerStub != null) {
            providerStub.stop(0);
        }
        if (providerExecutor != null) {
            providerExecutor.shutdownNow();
        }
    }

    @Test
    void healthEndpointReportsUp() throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void translateEndpointWithoutCredentialsIsRejected() throws IOException, InterruptedException {
        int requestsBefore = PROVIDER_REQUEST_COUNT.get();
        HttpResponse<String> response = postJsonWithoutCredentials(
                "{\"text\":\"Hello\",\"sourceLanguage\":\"English\"}");

        assertUnauthorizedWithBasicChallenge(response);
        assertProviderWasNotCalled(requestsBefore);
    }

    @Test
    void translateEndpointWithIncorrectCredentialsIsRejected() throws IOException, InterruptedException {
        int requestsBefore = PROVIDER_REQUEST_COUNT.get();
        HttpResponse<String> response = postJson(
                "{\"text\":\"Hello\",\"sourceLanguage\":\"English\"}",
                TEST_USERNAME,
                "incorrect-password");

        assertUnauthorizedWithBasicChallenge(response);
        assertProviderWasNotCalled(requestsBefore);
    }

    @Test
    void translateEndpointWithCorrectCredentialsReturnsProviderTranslation()
            throws IOException, InterruptedException {
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"text\":\"Hello\",\"sourceLanguage\":\"English\"}");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"translation\":\"سلام، كيداير؟\""));
        assertTrue(response.body().contains("\"model\":\"stub-model\""));
        assertTrue(response.body().contains("Moroccan Darija (Arabic script)"));
    }

    @Test
    void providerRejectionReturnsUsefulBadGatewayError()
            throws IOException, InterruptedException {
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"text\":\"trigger-rejection\",\"sourceLanguage\":\"English\"}");

        assertEquals(502, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"PROVIDER_REJECTED_REQUEST\""));
        assertTrue(response.body().contains("HTTP 429"));
    }

    @Test
    void emptyProviderResponseReturnsUsefulBadGatewayError()
            throws IOException, InterruptedException {
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"text\":\"trigger-empty\",\"sourceLanguage\":\"English\"}");

        assertEquals(502, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"PROVIDER_EMPTY_RESPONSE\""));
        assertTrue(response.body().contains("returned no translation"));
    }

    @Test
    void providerTimeoutReturnsUsefulGatewayTimeoutError()
            throws IOException, InterruptedException {
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"text\":\"trigger-timeout\",\"sourceLanguage\":\"English\"}");

        assertEquals(504, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"PROVIDER_TIMEOUT\""));
        assertTrue(response.body().contains("timed out"));
    }

    @Test
    void authenticatedTranslateEndpointRejectsBlankText() throws IOException, InterruptedException {
        int requestsBefore = PROVIDER_REQUEST_COUNT.get();
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"text\":\"   \",\"sourceLanguage\":\"English\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"VALIDATION_ERROR\""));
        assertProviderWasNotCalled(requestsBefore);
    }

    @Test
    void authenticatedTranslateEndpointRejectsMissingText() throws IOException, InterruptedException {
        int requestsBefore = PROVIDER_REQUEST_COUNT.get();
        HttpResponse<String> response = postJsonWithCorrectCredentials(
                "{\"sourceLanguage\":\"English\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("must not be blank"));
        assertProviderWasNotCalled(requestsBefore);
    }

    private HttpResponse<String> postJsonWithCorrectCredentials(String body)
            throws IOException, InterruptedException {
        return postJson(body, TEST_USERNAME, TEST_PASSWORD);
    }

    private HttpResponse<String> postJson(String body, String username, String password)
            throws IOException, InterruptedException {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));

        return send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/translate"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedCredentials)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    private HttpResponse<String> postJsonWithoutCredentials(String body)
            throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/translate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    private static void assertUnauthorizedWithBasicChallenge(HttpResponse<String> response) {
        assertEquals(401, response.statusCode());
        String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");
        assertTrue(challenge.startsWith("Basic "),
                () -> "Expected a Basic challenge but received: " + challenge);
        assertTrue(challenge.contains("realm=\"Darija Translator\""),
                () -> "Expected the Darija Translator realm but received: " + challenge);
    }

    private static void assertProviderWasNotCalled(int requestsBefore) {
        assertEquals(requestsBefore, PROVIDER_REQUEST_COUNT.get(),
                "Rejected backend requests must not reach the translation provider.");
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void handleProviderRequest(HttpExchange exchange) throws IOException {
        PROVIDER_REQUEST_COUNT.incrementAndGet();
        try {
            String apiKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            String requestBody = new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            if (!"integration-test-fake-key".equals(apiKey)
                    || !requestBody.contains("systemInstruction")
                    || !requestBody.contains("untrusted material to translate")) {
                sendStubResponse(exchange, 400, "{\"error\":\"invalid test request\"}");
            } else if (requestBody.contains("trigger-rejection")) {
                sendStubResponse(exchange, 429, "{\"error\":\"test quota exhausted\"}");
            } else if (requestBody.contains("trigger-empty")) {
                sendStubResponse(exchange, 200, "{\"candidates\":[]}");
            } else if (requestBody.contains("trigger-timeout")) {
                try {
                    Thread.sleep(2_000);
                    sendStubResponse(exchange, 200, successProviderResponse());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            } else {
                sendStubResponse(exchange, 200, successProviderResponse());
            }
        } finally {
            exchange.close();
        }
    }

    private static String successProviderResponse() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + "\"سلام، كيداير؟\"}]}}]}";
    }

    private static void sendStubResponse(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
