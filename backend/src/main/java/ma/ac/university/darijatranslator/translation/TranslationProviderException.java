package ma.ac.university.darijatranslator.translation;

public final class TranslationProviderException extends Exception {
    public enum Kind {
        TIMEOUT,
        REJECTED,
        EMPTY_RESPONSE,
        UNAVAILABLE,
        CONFIGURATION
    }

    private final Kind kind;

    public TranslationProviderException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public TranslationProviderException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
