package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Objects;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public final class AuthUtil {
    private AuthUtil() {
        // Prevent instantiation
    }

    public static String getIssuerUrl(final LoginType loginType, final LoginParams loginParams) {
        if (loginType == null || loginType.equals(LoginType.NONE)) {
            return null;
        }
        if (loginType.equals(LoginType.BUILDER_ID)) {
            return Constants.AWS_BUILDER_ID_URL;
        }

        if (Objects.isNull(loginParams) || Objects.isNull(loginParams.getLoginIdcParams())) {
            return null;
        }

        return loginParams.getLoginIdcParams().getUrl();
    }
}
