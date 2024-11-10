package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public enum SsoTokenChangedKind {
    EXPIRED("Expired"),
    REFRESHED("Refreshed");

    private final String value;

    SsoTokenChangedKind(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
