package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public record UpdateProfileParams(Profile profile, SsoSession ssoSession, UpdateProfileOptions options) { }
