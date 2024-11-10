package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public interface AuthStateManager {
    void toLoggedIn(LoginType loginType, LoginParams loginParams, String ssoTokenId);
    void toLoggedOut();
    void toExpired();
    AuthState getAuthState();
}
