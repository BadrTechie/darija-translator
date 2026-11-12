package ma.ac.university.darijatranslator.security;

import java.util.Set;

import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Registers the Jakarta Authentication module before the web application starts.
 */
public final class AuthenticationInitializer implements ServletContainerInitializer {
    static final String USERNAME_ENVIRONMENT_VARIABLE = "TRANSLATOR_DEMO_USERNAME";
    static final String PASSWORD_ENVIRONMENT_VARIABLE = "TRANSLATOR_DEMO_PASSWORD";
    static final String KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE = "keystore_password";

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext)
            throws ServletException {
        String username = requireEnvironmentVariable(USERNAME_ENVIRONMENT_VARIABLE);
        String password = requireEnvironmentVariable(PASSWORD_ENVIRONMENT_VARIABLE);
        requireEnvironmentVariable(KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE);

        try {
            AuthConfigFactory.getFactory().registerServerAuthModule(
                    new BasicAuthenticationModule(username, password),
                    servletContext);
        } catch (RuntimeException exception) {
            throw new ServletException(
                    "Jakarta Authentication module registration failed.",
                    exception);
        }
    }

    private static String requireEnvironmentVariable(String name) throws ServletException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new ServletException(
                    "Required authentication environment variable is missing: " + name);
        }
        return value;
    }

    static String firstMissingEnvironmentVariable() {
        String[] requiredVariables = {
                USERNAME_ENVIRONMENT_VARIABLE,
                PASSWORD_ENVIRONMENT_VARIABLE,
                KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE
        };

        for (String variable : requiredVariables) {
            String value = System.getenv(variable);
            if (value == null || value.isBlank()) {
                return variable;
            }
        }
        return null;
    }
}
