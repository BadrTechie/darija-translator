package ma.ac.university.darijatranslator.translation;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

/** Runtime-only Gemini settings. The API key is never logged or returned. */
public record ProviderConfiguration(
        URI apiBaseUri,
        String model,
        String apiKey,
        Duration timeout) {

    public static final String API_KEY_ENV = "GEMINI_API_KEY";
    public static final String MODEL_ENV = "GEMINI_MODEL";
    public static final String API_BASE_URL_ENV = "GEMINI_API_BASE_URL";
    public static final String TIMEOUT_SECONDS_ENV = "GEMINI_TIMEOUT_SECONDS";

    public static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    public static final String DEFAULT_API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta";
    public static final int DEFAULT_TIMEOUT_SECONDS = 20;

    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    public ProviderConfiguration {
        if (apiBaseUri == null || !apiBaseUri.isAbsolute()) {
            throw new IllegalArgumentException("Gemini API base URL must be absolute.");
        }
        if (model == null || !MODEL_NAME.matcher(model).matches()) {
            throw new IllegalArgumentException("Gemini model name is invalid.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "GEMINI_API_KEY is not configured on the backend.");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Gemini timeout must be greater than zero.");
        }
    }

    public static ProviderConfiguration fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static ProviderConfiguration fromEnvironment(Map<String, String> environment) {
        String apiKey = environment.get(API_KEY_ENV);
        String model = valueOrDefault(environment.get(MODEL_ENV), DEFAULT_MODEL);
        String baseUrl = stripTrailingSlashes(valueOrDefault(
                environment.get(API_BASE_URL_ENV), DEFAULT_API_BASE_URL));
        int timeoutSeconds = parseTimeout(environment.get(TIMEOUT_SECONDS_ENV));
        return new ProviderConfiguration(
                URI.create(baseUrl), model, apiKey, Duration.ofSeconds(timeoutSeconds));
    }

    URI generateContentUri() {
        return URI.create(apiBaseUri + "/models/" + model + ":generateContent");
    }

    private static int parseTimeout(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        try {
            int seconds = Integer.parseInt(configuredValue);
            if (seconds < 1 || seconds > 300) {
                throw new IllegalArgumentException(
                        "GEMINI_TIMEOUT_SECONDS must be between 1 and 300.");
            }
            return seconds;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "GEMINI_TIMEOUT_SECONDS must be a whole number.", exception);
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String stripTrailingSlashes(String value) {
        return value.replaceFirst("/+$", "");
    }
}
