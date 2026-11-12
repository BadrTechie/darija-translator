package ma.ac.university.darijatranslator.api;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Map<String, String> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("service", "darija-translator-backend");
        status.put("status", "UP");
        return status;
    }
}
