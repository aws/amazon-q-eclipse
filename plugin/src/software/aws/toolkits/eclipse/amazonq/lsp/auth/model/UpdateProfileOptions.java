package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public record UpdateProfileOptions(
        boolean createNonexistentProfile,
        boolean createNonexistentSsoSession,
        boolean ensureSsoAccountAccessScope,
        boolean updateSharedSsoSession) { }
