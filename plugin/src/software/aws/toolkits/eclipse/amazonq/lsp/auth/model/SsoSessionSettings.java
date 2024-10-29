package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public record SsoSessionSettings(
        @SerializedName("sso_start_url") String ssoStartUrl,
        @SerializedName("sso_region") String ssoRegion,
        @SerializedName("sso_registration_scopes") List<String> ssoRegistrationScopes) { }
