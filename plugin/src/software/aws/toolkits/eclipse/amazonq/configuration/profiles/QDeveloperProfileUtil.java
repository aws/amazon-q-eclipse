// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.broker.events.QDeveloperProfileState;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams.ExpectedResponseType;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;
import software.aws.toolkits.eclipse.amazonq.views.ViewConstants;
import software.aws.toolkits.eclipse.amazonq.views.model.QDeveloperProfile;
import software.aws.toolkits.eclipse.amazonq.views.model.UpdateConfigurationParams;

public final class QDeveloperProfileUtil {

    private static final QDeveloperProfileUtil INSTANCE;
    private List<QDeveloperProfile> profiles;
    private QDeveloperProfile savedDeveloperProfile;
    private QDeveloperProfile selectedDeveloperProfile;
    private CompletableFuture<Void> profileSelectionTask;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        INSTANCE = new QDeveloperProfileUtil();
    }

    public static QDeveloperProfileUtil getInstance() {
        return INSTANCE;
    }

    private QDeveloperProfileUtil() { // prevent initialization
        try {
            savedDeveloperProfile = Optional
                    .ofNullable(Activator.getPluginStore().get(ViewConstants.Q_DEVELOPER_PROFILE_SELECTION_KEY))
                    .map(json -> deserializeProfile(json)).orElse(null);
        } catch (Exception e) {
            Activator.getLogger().error("Failed to deserialize developer profile", e);
        }
        profileSelectionTask = new CompletableFuture<>();
        profiles = new ArrayList<QDeveloperProfile>();
    }

    private QDeveloperProfile deserializeProfile(final String json) {
        try {
            return OBJECT_MAPPER.readValue(json, QDeveloperProfile.class);
        } catch (JsonProcessingException e) {
            Activator.getLogger().error("Error deserializing profile", e);
            return null;
        }
    }

    private String serializeProfile(final QDeveloperProfile developerProfile) {
        try {
            return OBJECT_MAPPER.writeValueAsString(developerProfile);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void initialize() {
        if (savedDeveloperProfile != null) {
            setDeveloperProfile(savedDeveloperProfile);
        }
    }

    public synchronized List<QDeveloperProfile> queryForDeveloperProfiles(final boolean tryApplyCachedProfile) {
        CompletableFuture<List<QDeveloperProfile>> profilesFuture = Activator.getLspProvider().getAmazonQServer()
                .thenCompose(server -> {
                    GetConfigurationFromServerParams params = new GetConfigurationFromServerParams(
                            ExpectedResponseType.Q_DEVELOPER_PROFILE);
                    CompletableFuture<LspServerConfigurations<QDeveloperProfile>> response = server
                            .getConfigurationFromServer(params);
                    return response;
                }).thenApply(this::processConfigurations).exceptionally(throwable -> {
                    Activator.getLogger().error("Error occurred while fetching the list of Q Developer Profile: ",
                            throwable);
                    throw new AmazonQPluginException(throwable);
                }).thenApply(result -> {
                    return handleSelectedProfile(result, tryApplyCachedProfile);
                });
        try {
            profiles = profilesFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Activator.getLogger().error("Failed to fetch developer profiles: ", e);
        }

        return profiles;
    }

    public synchronized CompletableFuture<Void> getProfileSelectionTaskFuture() {
        if (profileSelectionTask != null && !profileSelectionTask.isDone()) {
            return profileSelectionTask;
        }
        profileSelectionTask = new CompletableFuture<Void>();
        return profileSelectionTask;
    }

    private boolean isValidProfile(final QDeveloperProfile profile) {
        return profile != null && StringUtils.isNotBlank(profile.getName());
    }

    private List<QDeveloperProfile> handleSelectedProfile(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile) {
        boolean isProfileSet = false;
        if (profiles.size() <= 1) {
            isProfileSet = handleSingleOrNoProfile(profiles, tryApplyCachedProfile);
        } else {
            isProfileSet = handleMultipleProfiles(profiles, tryApplyCachedProfile);
        }

        if (!isProfileSet) {
            Activator.getEventBroker().post(QDeveloperProfileState.class, QDeveloperProfileState.AVAILABLE);
        }
        return profiles;
    }

    private boolean handleSingleOrNoProfile(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile) {
        if (!profiles.isEmpty() && tryApplyCachedProfile) {
            setDeveloperProfile(profiles.get(0));
            return true;
        }
        return false;
    }

    private boolean handleMultipleProfiles(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile) {
        boolean isProfileSelected = false;
        if (selectedDeveloperProfile != null) {
            isProfileSelected = profiles.stream()
                    .anyMatch(profile -> profile.getArn().equals(selectedDeveloperProfile.getArn()));

            if (isProfileSelected && tryApplyCachedProfile) {
                setDeveloperProfile(selectedDeveloperProfile);
            }
        }
        return isProfileSelected;
    }

    private List<QDeveloperProfile> processConfigurations(
            final LspServerConfigurations<QDeveloperProfile> configurations) {
        return Optional.ofNullable(configurations).map(
                config -> config.getConfigurations().stream().filter(this::isValidProfile).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<QDeveloperProfile> getDeveloperProfiles(final boolean tryApplyCachedProfile) {
        if (profiles != null && !profiles.isEmpty()) {
            return profiles;
        }
        return queryForDeveloperProfiles(tryApplyCachedProfile);
    }

    public void setDeveloperProfile(final QDeveloperProfile developerProfile) {
        if (developerProfile == null || (selectedDeveloperProfile != null
                && selectedDeveloperProfile.getArn().equals(developerProfile.getArn()))) {
            return;
        }

        selectedDeveloperProfile = developerProfile;
        saveSelectedProfile();

        String section = "aws.q";
        Map<String, Object> settings = Map.of("profileArn", selectedDeveloperProfile.getArn());
        Activator.getLspProvider().getAmazonQServer()
                .thenCompose(server -> server.updateConfiguration(new UpdateConfigurationParams(section, settings)))
                .thenRun(() -> {
                    showNotification(selectedDeveloperProfile.getName());
                    Activator.getEventBroker().post(QDeveloperProfileState.class, QDeveloperProfileState.SELECTED);
                    if (profileSelectionTask != null) {
                        profileSelectionTask.complete(null);
                    }
                    profiles = null;
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Error occurred while setting Q Developer Profile: ", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    private void showNotification(final String developerProfileName) {
        Display.getDefault().asyncExec(() -> {
            AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                    Constants.IDE_DEVELOPER_PROFILES_NOTIFICATION_TITLE,
                    String.format(Constants.IDE_DEVELOPER_PROFILES_NOTIFICATION_BODY_TEMPLATE, developerProfileName));
            notification.open();
        });
    }

    public void clearSelectedProfile() {
        Activator.getPluginStore().remove(ViewConstants.Q_DEVELOPER_PROFILE_SELECTION_KEY);
        selectedDeveloperProfile = null;
    }

    private void saveSelectedProfile() {
        String serializedSelectedProfile = serializeProfile(selectedDeveloperProfile);
        if (serializedSelectedProfile != null) {
            Activator.getPluginStore().put(ViewConstants.Q_DEVELOPER_PROFILE_SELECTION_KEY,
                    serializedSelectedProfile);
        }
    }

    public boolean isProfileSelectionRequired() {
        if (profiles == null || profiles.isEmpty()) {
            queryForDeveloperProfiles(false);

            if (profiles.size() == 1) {
                handleSingleOrNoProfile(profiles, true);
            }
        }
        return profiles.size() > 1;
    }

    public QDeveloperProfile getSelectedProfile() {
        return selectedDeveloperProfile;
    }

}
