package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public final class AuthPluginStore {

    private PluginStore pluginStore;

    public AuthPluginStore(final PluginStore pluginStore) {
        this.pluginStore = pluginStore;
    }

    public void setLoginType(final LoginType loginType) {
        pluginStore.put(Constants.LOGIN_TYPE_KEY, loginType.name());
    }

    public LoginType getLoginType() {
        String storedValue = pluginStore.get(Constants.LOGIN_TYPE_KEY);

         if (storedValue.equals(LoginType.BUILDER_ID.name())) {
            return LoginType.BUILDER_ID;
        } else if (storedValue.equals(LoginType.IAM_IDENTITY_CENTER.name())) {
            return LoginType.IAM_IDENTITY_CENTER;
        } else {
            return LoginType.NONE;
        }
    }

    public void setLoginParams(final LoginParams loginParams) {
        pluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, loginParams.getLoginIdcParams());
    }

    public LoginParams getLoginParams() {
        LoginIdcParams loginIdcParams = pluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class);
        LoginParams loginParams = new LoginParams();
        loginParams.setLoginIdcParams(loginIdcParams);
        return loginParams;
    }

    public void clear() {
        pluginStore.remove(Constants.LOGIN_TYPE_KEY);
        pluginStore.remove(Constants.LOGIN_IDC_PARAMS_KEY);
    }
}
