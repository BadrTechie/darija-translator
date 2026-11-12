package ma.ac.university.darijatranslator.api;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.ac.university.darijatranslator.translation.GeminiTranslationClient;
import ma.ac.university.darijatranslator.translation.ProviderConfiguration;
import ma.ac.university.darijatranslator.translation.TranslationProviderException;

@Path("/translate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TranslatorResource {

    @POST
    public Response translate(TranslationRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", "Field 'text' is required and must not be blank.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        String sourceLanguage = request.getSourceLanguage();
        if (sourceLanguage == null || sourceLanguage.isBlank()) {
            sourceLanguage = "unspecified";
        }

        try {
            String translation = new GeminiTranslationClient()
                    .translate(request.getText(), sourceLanguage);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("translation", translation);
            result.put("sourceLanguage", sourceLanguage);
            result.put("targetLanguage", "Moroccan Darija (Arabic script)");
            result.put("model", configuredModel());
            return Response.ok(result).build();
        } catch (TranslationProviderException exception) {
            return providerError(exception);
        }
    }

    private static Response providerError(TranslationProviderException exception) {
        Response.Status status;
        String code;
        switch (exception.kind()) {
            case TIMEOUT -> {
                status = Response.Status.GATEWAY_TIMEOUT;
                code = "PROVIDER_TIMEOUT";
            }
            case CONFIGURATION -> {
                status = Response.Status.SERVICE_UNAVAILABLE;
                code = "PROVIDER_NOT_CONFIGURED";
            }
            case REJECTED -> {
                status = Response.Status.BAD_GATEWAY;
                code = "PROVIDER_REJECTED_REQUEST";
            }
            case EMPTY_RESPONSE -> {
                status = Response.Status.BAD_GATEWAY;
                code = "PROVIDER_EMPTY_RESPONSE";
            }
            default -> {
                status = Response.Status.BAD_GATEWAY;
                code = "PROVIDER_UNAVAILABLE";
            }
        }

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", code);
        error.put("message", exception.getMessage());
        return Response.status(status).entity(error).build();
    }

    private static String configuredModel() {
        String model = System.getenv(ProviderConfiguration.MODEL_ENV);
        return model == null || model.isBlank()
                ? ProviderConfiguration.DEFAULT_MODEL
                : model.trim();
    }
}
