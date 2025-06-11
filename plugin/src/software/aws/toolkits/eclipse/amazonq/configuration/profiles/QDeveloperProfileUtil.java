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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.broker.events.QDeveloperProfileState;
import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams.ExpectedResponseType;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;
import software.aws.toolkits.eclipse.amazonq.views.ViewConstants;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;
import software.aws.toolkits.eclipse.amazonq.views.model.QDeveloperProfile;
import software.aws.toolkits.eclipse.amazonq.views.model.UpdateConfigurationParams;

public final class QDeveloperProfileUtil {

    private static final QDeveloperProfileUtil INSTANCE;
    private QDeveloperProfile savedDeveloperProfile;
    private QDeveloperProfile selectedDeveloperProfile;
    private CompletableFuture<Void> profileSelectionTask;
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
    private List<QDeveloperProfile> profiles;
    private ReentrantLock profilesLock = new ReentrantLock(true);

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
                    .map(json -> {
                        try {
                            if (isValidSerializedProfile(json)) {
                                return deserializeProfile(json);
                            } else {
                                Activator.getLogger().error("Cached profile has invalid format");
                            }
                        } catch (final JsonProcessingException e) {
                            Activator.getLogger().error("Failed to process cached profile", e);
                        }
                        return null;
                    }).orElse(null);

        } catch (Exception e) {
            Activator.getLogger().error("Failed to deserialize developer profile", e);
        }
        profileSelectionTask = new CompletableFuture<>();
        profiles = new ArrayList<>();
    }

    private boolean isValidSerializedProfile(final String profile) throws JsonProcessingException {
        JsonNode node = OBJECT_MAPPER.readTree(profile);
        return node.has("arn") && isValidArn(node.get("arn").asText()) && node.has("name")
                && StringUtils.isNotBlank(node.get("name").asText()) && node.has("accountId")
                && isValidAccountId(node.get("accountId").asText()) && node.has("region")
                && node.get("identityDetails").has("region")
                && isValidRegion(node.get("identityDetails").get("region").asText());
    }

    private QDeveloperProfile deserializeProfile(final String json) throws JsonProcessingException {
        QDeveloperProfile deserializedProfile = OBJECT_MAPPER.readValue(json, QDeveloperProfile.class);

        if (!isValidProfile(deserializedProfile)) {
            throw new JsonProcessingException("Cached profile has invalid data") {
                private static final long serialVersionUID = 1L;
            };
        }
        return deserializedProfile;
    }

    private String serializeProfile(final QDeveloperProfile developerProfile) throws JsonProcessingException {
        if (!isValidProfile(developerProfile)) {
            throw new JsonProcessingException("Developer profile has invalid data") {
                private static final long serialVersionUID = 1L;
            };
        }

        return OBJECT_MAPPER.writeValueAsString(developerProfile);
    }

    public void initialize() {
        if (savedDeveloperProfile != null) {
            selectedDeveloperProfile = savedDeveloperProfile;
            queryForDeveloperProfilesFuture(true, true).exceptionally(throwable -> {
                Activator.getLogger().error(
                        "Plugin initialization with saved developer profile failed. Prompting user to log back in.");
                Activator.getLoginService().logout();
                return null;
            }).thenAccept(result -> {
                CustomizationUtil.validateCurrentCustomization();
            });
            savedDeveloperProfile = null;
        } else {
            Activator.getLogger().info("No saved developer profile found, logging out");
            profileSelectionTask.complete(null);
            Activator.getLoginService().logout();
        }
    }

    public synchronized CompletableFuture<List<QDeveloperProfile>> queryForDeveloperProfilesFuture(
            final boolean tryApplyCachedProfile) {
        return queryForDeveloperProfilesFuture(tryApplyCachedProfile, false);
    }

    private synchronized CompletableFuture<List<QDeveloperProfile>> queryForDeveloperProfilesFuture(
            final boolean tryApplyCachedProfile, final boolean applyProfileUnconditionally) {
        return Activator.getLspProvider().getAmazonQServer()
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
                    return handleSelectedProfile(result, tryApplyCachedProfile, applyProfileUnconditionally);
                });
    }

    public synchronized List<QDeveloperProfile> queryForDeveloperProfiles(final boolean tryApplyCachedProfile) throws ExecutionException {
        try {
            return queryForDeveloperProfilesFuture(tryApplyCachedProfile, false).get();
        } catch (InterruptedException e) {
            Activator.getLogger().error("Interrupted when fetching profile: ", e);
        }

        return new ArrayList<>();
    }

    public synchronized CompletableFuture<Void> getProfileSelectionTaskFuture() {
        if (profileSelectionTask != null && !profileSelectionTask.isDone()) {
            return profileSelectionTask;
        }
        profileSelectionTask = new CompletableFuture<Void>();
        return profileSelectionTask;
    }

    private boolean isValidProfile(final QDeveloperProfile profile) {
        return profile != null && StringUtils.isNotBlank(profile.getName()) && isValidAccountId(profile.getAccountId())
                && isValidArn(profile.getArn()) && isValidRegion(profile.getRegion());
    }

    private boolean isValidAccountId(final String accountId) {
        return accountId != null && accountId.matches("^\\d{12}$");
    }

    private boolean isValidArn(final String arn) {
        return arn != null && arn.matches("^arn:aws:codewhisperer:[a-z]{2}-[a-z]+-\\d:\\d{12}:profile/[A-Z0-9]+$");
    }

    private boolean isValidRegion(final String region) {
        return region != null && region
                .matches("^[a-z]{2}-(central|north|south|east|west|northeast|southeast|northwest|southwest)-(\\d)$");
    }

    private List<QDeveloperProfile> handleSelectedProfile(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile, final boolean applyProfileUnconditionally) {
        boolean isProfileSet = false;
        if (profiles.size() <= 1) {
            isProfileSet = handleSingleOrNoProfile(profiles, tryApplyCachedProfile, applyProfileUnconditionally);
        } else {
            isProfileSet = handleMultipleProfiles(profiles, tryApplyCachedProfile, applyProfileUnconditionally);
        }

        if (!isProfileSet) {
            setProfiles(profiles);
            Activator.getEventBroker().post(QDeveloperProfileState.class, QDeveloperProfileState.AVAILABLE);
        }
        return profiles;
    }

    private List<QDeveloperProfile> getProfiles() {
        try {
            profilesLock.lock();
            return profiles;
        } finally {
            profilesLock.unlock();
        }
    }

    private void setProfiles(final List<QDeveloperProfile> profiles) {
        try {
            profilesLock.lock();
            this.profiles = profiles;
        } finally {
            profilesLock.unlock();
        }
    }

    private boolean handleSingleOrNoProfile(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile, final boolean applyProfileUnconditionally) {
        if (!profiles.isEmpty() && tryApplyCachedProfile) {
            setDeveloperProfile(profiles.get(0), true, applyProfileUnconditionally);
            return true;
        }
        return false;
    }

    private boolean handleMultipleProfiles(final List<QDeveloperProfile> profiles,
            final boolean tryApplyCachedProfile, final boolean applyProfileUnconditionally) {
        boolean isProfileSelected = false;
        if (selectedDeveloperProfile != null) {
            isProfileSelected = profiles.stream()
                    .anyMatch(profile -> {
                        return profile.getArn().equals(selectedDeveloperProfile.getArn());
                    });

            if (isProfileSelected && tryApplyCachedProfile) {
                setDeveloperProfile(selectedDeveloperProfile, true, applyProfileUnconditionally);
            }
        }
        return isProfileSelected;
    }

    private List<QDeveloperProfile> processConfigurations(
            final LspServerConfigurations<QDeveloperProfile> configurations) {
        return Optional.ofNullable(configurations).map(
                config -> {
                    return config.getConfigurations().stream().filter(this::isValidProfile)
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }

    public List<QDeveloperProfile> getDeveloperProfiles() {
        List<QDeveloperProfile> profiles = getProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            return profiles;
        }

        try {
            return queryForDeveloperProfiles(false);
        } catch (Exception e) {
            Activator.getLogger().error("Interupted while fetching profiles: " + e);
        }

        return null;
    }

    public CompletableFuture<Void> setDeveloperProfile(final QDeveloperProfile developerProfile,
            final boolean updateCustomization) {
        return setDeveloperProfile(developerProfile, updateCustomization, false);
    }

    private CompletableFuture<Void> setDeveloperProfile(final QDeveloperProfile developerProfile,
            final boolean updateCustomization, final boolean applyProfileUnconditionally) {
        if (developerProfile == null || (!applyProfileUnconditionally && selectedDeveloperProfile != null
                && selectedDeveloperProfile.getArn().equals(developerProfile.getArn()))) {
            return CompletableFuture.completedFuture(null);
        }

        selectedDeveloperProfile = developerProfile;
        saveSelectedProfile();

        String section = "aws.q";
        Map<String, Object> settings = Map.of("profileArn", selectedDeveloperProfile.getArn());
        return Activator.getLspProvider().getAmazonQServer()
                .thenCompose(server -> server.updateConfiguration(new UpdateConfigurationParams(section, settings)))
                .thenRun(() -> {
                    showNotification(selectedDeveloperProfile.getName());
                    Activator.getEventBroker().post(QDeveloperProfileState.class, QDeveloperProfileState.SELECTED);
                    if (profileSelectionTask != null) {
                        profileSelectionTask.complete(null);
                    }
                    setProfiles(null);

                    Customization currentCustomization = Activator.getPluginStore()
                            .getObject(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY, Customization.class);

                    if (updateCustomization && currentCustomization != null
                            && !selectedDeveloperProfile.getArn().equals(currentCustomization.getProfile().getArn())) {
                        Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
                        ThreadingUtils
                                .executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification());
                        Display.getDefault().asyncExec(
                                () -> CustomizationUtil.showNotification(Constants.DEFAULT_Q_FOUNDATION_DISPLAY_NAME));
                    }
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
        try {
            String serializedSelectedProfile = serializeProfile(selectedDeveloperProfile);

            if (serializedSelectedProfile != null) {
                Activator.getPluginStore().put(ViewConstants.Q_DEVELOPER_PROFILE_SELECTION_KEY,
                        serializedSelectedProfile);
            }
        } catch (final JsonProcessingException e) {
            Activator.getLogger().error("Failed to cache Q developer profile");
        }
    }

    public boolean isProfileSelectionRequired() {
        if (profiles == null || profiles.isEmpty()) {
            try {
                queryForDeveloperProfiles(false);
            } catch (Exception e) {
                Activator.getLogger().error("Interrupted when fetching profile: ", e);
            }

            if (profiles.size() == 1) {
                handleSingleOrNoProfile(profiles, true, false);
            }
        }
        return profiles.size() > 1;
    }

    public QDeveloperProfile getSelectedProfile() {
        return selectedDeveloperProfile;
    }

}
