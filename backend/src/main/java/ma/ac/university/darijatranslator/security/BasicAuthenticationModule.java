package ma.ac.university.darijatranslator.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jakarta Authentication server module for the project's demo HTTP Basic login.
 */
public final class BasicAuthenticationModule implements ServerAuthModule {
    static final String TRANSLATOR_ROLE = "translator-user";

    private static final String MANDATORY_PROPERTY =
            "jakarta.security.auth.message.MessagePolicy.isMandatory";
    private static final String CHALLENGE =
            "Basic realm=\"Darija Translator\", charset=\"UTF-8\"";
    private static final Class<?>[] SUPPORTED_MESSAGE_TYPES = {
            HttpServletRequest.class, HttpServletResponse.class
    };

    private final String expectedUsername;
    private final String expectedPassword;
    private CallbackHandler callbackHandler;

    public BasicAuthenticationModule(String expectedUsername, String expectedPassword) {
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
    }

    @Override
    public void initialize(
            MessagePolicy requestPolicy,
            MessagePolicy responsePolicy,
            CallbackHandler callbackHandler,
            Map<String, Object> options) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES.clone();
    }

    @Override
    public AuthStatus validateRequest(
            MessageInfo messageInfo,
            Subject clientSubject,
            Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        String authorization = request.getHeader("Authorization");

        if (authorization == null || authorization.isBlank()) {
            return isAuthenticationMandatory(messageInfo)
                    ? reject(response)
                    : AuthStatus.SUCCESS;
        }

        Credentials credentials = decodeBasicCredentials(authorization);
        if (credentials == null
                || !constantTimeEquals(expectedUsername, credentials.username())
                || !constantTimeEquals(expectedPassword, credentials.password())) {
            return reject(response);
        }

        establishIdentity(clientSubject);
        return AuthStatus.SUCCESS;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }

    private void establishIdentity(Subject clientSubject) throws AuthException {
        Callback[] callbacks = {
                new CallerPrincipalCallback(clientSubject, expectedUsername),
                new GroupPrincipalCallback(clientSubject, new String[] { TRANSLATOR_ROLE })
        };

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException exception) {
            AuthException authException = new AuthException("Could not establish the authenticated identity.");
            authException.initCause(exception);
            throw authException;
        }
    }

    private static boolean isAuthenticationMandatory(MessageInfo messageInfo) {
        return Boolean.parseBoolean(
                String.valueOf(messageInfo.getMap().get(MANDATORY_PROPERTY)));
    }

    private static AuthStatus reject(HttpServletResponse response) throws AuthException {
        response.setHeader("WWW-Authenticate", CHALLENGE);
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException exception) {
            AuthException authException = new AuthException("Could not send the authentication challenge.");
            authException.initCause(exception);
            throw authException;
        }
        return AuthStatus.SEND_CONTINUE;
    }

    private static Credentials decodeBasicCredentials(String authorization) {
        int separator = authorization.indexOf(' ');
        if (separator < 1 || !"Basic".equalsIgnoreCase(authorization.substring(0, separator))) {
            return null;
        }

        String encodedCredentials = authorization.substring(separator + 1).trim();
        if (encodedCredentials.isEmpty()) {
            return null;
        }

        final String decodedCredentials;
        try {
            decodedCredentials = new String(
                    Base64.getDecoder().decode(encodedCredentials),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        int colon = decodedCredentials.indexOf(':');
        if (colon < 0) {
            return null;
        }

        return new Credentials(
                decodedCredentials.substring(0, colon),
                decodedCredentials.substring(colon + 1));
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private record Credentials(String username, String password) {
    }
}
