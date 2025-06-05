// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;

import software.amazon.awssdk.regions.servicemetadata.OidcServiceMetadata;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.configuration.profiles.QDeveloperProfileUtil;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.AwsRegion;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;
import software.aws.toolkits.eclipse.amazonq.views.model.QDeveloperProfile;

public class LoginViewActionHandler implements ViewActionHandler {

    private static final JsonHandler JSON_HANDLER = new JsonHandler();
    private static final ThemeDetector THEME_DETECTOR = new ThemeDetector();
    private Future<?> loginTask;
    private boolean isLoginTaskRunning = false;

    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Object params = parsedCommand.getParams();
        switch (parsedCommand.getCommand()) {
        case LOGIN_BUILDER_ID:
        case LOGIN_IDC:
            if (isLoginTaskRunning) {
                loginTask.cancel(true);
            }
            isLoginTaskRunning = true;
            loginTask = ThreadingUtils.executeAsyncTaskAndReturnFuture(() -> {
                try {
                    if (parsedCommand.getCommand() == Command.LOGIN_BUILDER_ID) {
                        Activator.getLoginService().login(LoginType.BUILDER_ID, new LoginParams()).get();
                    } else {
                        LoginIdcParams loginIdcParams = JSON_HANDLER.convertObject(params, LoginIdcParams.class);
                        var url = loginIdcParams.getUrl();
                        var region = loginIdcParams.getRegion();
                        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(region)) {
                            throw new IllegalArgumentException("Url/Region parameters cannot be null or empty");
                        }
                        Activator.getLoginService().login(LoginType.IAM_IDENTITY_CENTER,
                                new LoginParams().setLoginIdcParams(loginIdcParams)).get();
                        if (QDeveloperProfileUtil.getInstance().isProfileSelectionRequired()) {
                            Map<String, Object> profilesData = new HashMap<>();
                            profilesData.put("profiles",
                                    QDeveloperProfileUtil.getInstance().getDeveloperProfiles());
                            Display.getDefault().asyncExec(() -> {
                                browser.execute(String.format("ideClient.handleProfiles(%s)",
                                        JSON_HANDLER.serialize(profilesData)));
                            });
                        }
                    }
                    isLoginTaskRunning = false;
                } catch (Exception e) {
                    isLoginTaskRunning = false;
                    Activator.getLogger().error("Failed to update token", e);
                }
            });
            break;
        case CANCEL_LOGIN:
            if (isLoginTaskRunning) {
                loginTask.cancel(true);
                isLoginTaskRunning = false;
            }
            break;
        case ON_LOAD:
            OidcServiceMetadata oidcMetadata = new OidcServiceMetadata();
            String regions = "["
                    + oidcMetadata.regions().stream().filter(region -> region.metadata().partition().id().equals("aws"))
                            .map(AwsRegion::from).map(AwsRegion::toString).collect(Collectors.joining(","))
                    + "]";
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
                        existConnections: [],
                        profiles: []
                    }
                        """, "START", regions).stripIndent();
            browser.execute("changeTheme(" + THEME_DETECTOR.isDarkTheme() + ");");
            browser.execute(String.format("ideClient.prepareUi(%s)", js));
            browser.execute("ideClient.updateAuthorization('')");
            break;
        case ON_SELECT_PROFILE:
            QDeveloperProfile developerProfile = JSON_HANDLER.convertObject(params, QDeveloperProfile.class);
            QDeveloperProfileUtil.getInstance().setDeveloperProfile(developerProfile, true).thenRun(() -> {
                CustomizationUtil.validateCurrentCustomization();
            });
            break;
        default:
            Activator.getLogger()
                    .error("Unexpected command received from Amazon Q Login: " + parsedCommand.getCommand());
            break;
        }
    }
}
