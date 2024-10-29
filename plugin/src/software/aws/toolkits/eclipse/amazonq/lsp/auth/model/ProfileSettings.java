package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.google.gson.annotations.SerializedName;

public record ProfileSettings(@SerializedName("region") String region, @SerializedName("sso_session") String ssoSession) { }
