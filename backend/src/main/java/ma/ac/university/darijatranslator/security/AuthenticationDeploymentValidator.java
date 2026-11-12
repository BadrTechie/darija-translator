package ma.ac.university.darijatranslator.security;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Makes missing authentication configuration a CDI deployment failure.
 */
public final class AuthenticationDeploymentValidator implements Extension {

    void validateConfiguration(@Observes AfterDeploymentValidation validation) {
        String missingVariable = AuthenticationInitializer.firstMissingEnvironmentVariable();
        if (missingVariable != null) {
            validation.addDeploymentProblem(new IllegalStateException(
                    "Required authentication environment variable is missing: "
                            + missingVariable));
        }
    }
}
