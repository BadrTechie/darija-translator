package ma.ac.university.darijatranslator.api;

public class TranslationRequest {
    private String text;
    private String sourceLanguage;

    public TranslationRequest() {
        // Required by JSON-B.
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }
}
