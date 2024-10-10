// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.stream.Collectors;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import software.amazon.awssdk.regions.servicemetadata.OidcServiceMetadata;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.AwsRegion;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class LoginViewActionHandler implements ViewActionHandler {

    private static final JsonHandler JSON_HANDLER = new JsonHandler();

    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Object params = parsedCommand.getParams();
        switch (parsedCommand.getCommand()) {
            case LOGIN_BUILDER_ID:
                ThreadingUtils.executeAsyncTask(() -> {
                    try {
                        DefaultLoginService.getInstance().login(LoginType.BUILDER_ID, new LoginParams()).get();
                        Display.getDefault().asyncExec(() -> {
                            browser.setText("Login succeeded");
                            AmazonQView.showView(AmazonQChatWebview.ID);
                        });
                    } catch (Exception e) {
                        PluginLogger.error("Failed to update token", e);
                    }
                });
                break;
            case LOGIN_IDC:
                PluginLogger.info("loginIdc command received");
                ThreadingUtils.executeAsyncTask(() -> {
                    try {
                        LoginIdcParams loginIdcParams = JSON_HANDLER.convertObject(params, LoginIdcParams.class);
                        var url = loginIdcParams.getUrl();
                        var region = loginIdcParams.getRegion();
                        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(region)) {
                            throw new IllegalArgumentException("Url/Region parameters cannot be null or empty");
                        }
                        DefaultLoginService.getInstance().login(LoginType.IAM_IDENTITY_CENTER, new LoginParams().setLoginIdcParams(loginIdcParams)).get();
                        Display.getDefault().asyncExec(() -> {
                            browser.setText("Login succeeded");
                            AmazonQView.showView(AmazonQChatWebview.ID);
                        });
                    } catch (Exception e) {
                        PluginLogger.error("Failed to update token", e);
                    }
                });
                break;
            case CANCEL_LOGIN:
                PluginLogger.info("cancelLogin command received");
                break;
            case ON_LOAD:
                OidcServiceMetadata oidcMetadata = new OidcServiceMetadata();
                String regions = "[" + oidcMetadata.regions().stream()
                        .filter(region -> region.metadata().partition().id().equals("aws"))
                        .map(AwsRegion::from)
                        .map(AwsRegion::toString)
                        .collect(Collectors.joining(",")) + "]";
                var js = String.format("""
                        {
                            stage: '%s',
                            regions: %s,
                            cancellable: false,
                            idcInfo: {
                                profileName: '',
                                startUrl: '',
                                region: 'us-east-1'
                            },
                            feature: 'q',
                            existConnections: []
                        }
                            """, "START", regions).stripIndent();
                browser.execute(String.format("ideClient.prepareUi(%s)", js));
                break;
            default:
                System.out.println("Unknown command: " + parsedCommand.getCommand());
                break;
        }
    }
}
