// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.aws.toolkits.eclipse.amazonq.chat.ChatAsyncResultManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GetSerializedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GetSerializedChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.SerializedChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ShowSaveFileDialogParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ShowSaveFileDialogResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoTokenChangedKind;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoTokenChangedParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.lsp.model.OpenFileDiffParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.OpenTabParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.OpenTabResult;
import software.aws.toolkits.eclipse.amazonq.lsp.model.OpenTabUiResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.SsoProfileData;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TextCompareInput;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.DefaultTelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

@SuppressWarnings("restriction")
public class AmazonQLspClientImpl extends LanguageClientImpl implements AmazonQLspClient {

    @Override
    public final CompletableFuture<ConnectionMetadata> getConnectionMetadata() {
        return CompletableFuture.supplyAsync(() -> {
            SsoProfileData sso = new SsoProfileData();
            String startUrl = Constants.AWS_BUILDER_ID_URL;
            AuthState authState = Activator.getLoginService().getAuthState();
            if (authState.issuerUrl() != null && !authState.issuerUrl().isBlank()) {
                startUrl = authState.issuerUrl();
            }
            sso.setStartUrl(startUrl);
            ConnectionMetadata metadata = new ConnectionMetadata();
            metadata.setSso(sso);
            return metadata;
        });
    }

    @Override
    public final CompletableFuture<List<Object>> configuration(final ConfigurationParams configurationParams) {
        if (configurationParams.getItems().size() == 0) {
            return CompletableFuture.completedFuture(null);
        }
        List<Object> output = new ArrayList<>();
        configurationParams.getItems().forEach(item -> {
            if (item.getSection().equals(Constants.LSP_Q_CONFIGURATION_KEY)) {
                Customization storedCustomization = Activator.getPluginStore().getObject(
                        Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY,
                        Customization.class);
                Map<String, Object> qConfig = new HashMap<>();
                qConfig.put(Constants.LSP_CUSTOMIZATION_CONFIGURATION_KEY, Objects.nonNull(storedCustomization) ? storedCustomization.getArn() : null);
                qConfig.put(Constants.LSP_ENABLE_TELEMETRY_EVENTS_CONFIGURATION_KEY, false);
                qConfig.put(Constants.LSP_OPT_OUT_TELEMETRY_CONFIGURATION_KEY, !DefaultTelemetryService.telemetryEnabled());
                Map<String, Object> projectContextConfig = new HashMap<>();
                boolean indexingSetting = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.WORKSPACE_INDEX);
                boolean gpuIndexingSetting = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.USE_GPU_FOR_INDEXING);
                int indexThreadsSetting = Activator.getDefault().getPreferenceStore().getInt(AmazonQPreferencePage.INDEX_WORKER_THREADS);
                projectContextConfig.put(Constants.LSP_INDEXING_CONFIGURATION_KEY, indexingSetting);
                projectContextConfig.put(Constants.LSP_GPU_INDEXING_CONFIGURATION_KEY, gpuIndexingSetting);
                projectContextConfig.put(Constants.LSP_INDEX_THREADS_CONFIGURATION_KEY, indexThreadsSetting);
                qConfig.put(Constants.LSP_PROJECT_CONTEXT_CONFIGURATION_KEY, projectContextConfig);
                output.add(qConfig);
                Activator.getLspProvider().activate(AmazonQLspServer.class);
            } else if (item.getSection().equals(Constants.LSP_CW_CONFIGURATION_KEY)) {
                Map<String, Boolean> cwConfig = new HashMap<>();
                boolean shareContentSetting = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.Q_DATA_SHARING);
                boolean referencesEnabled = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.CODE_REFERENCE_OPT_IN)
                        && Activator.getLoginService().getAuthState().loginType().equals(LoginType.BUILDER_ID);
                cwConfig.put(Constants.LSP_CW_OPT_OUT_KEY, shareContentSetting);
                cwConfig.put(Constants.LSP_CODE_REFERENCES_OPT_OUT_KEY, referencesEnabled);
                output.add(cwConfig);
            }
        });
        return CompletableFuture.completedFuture(output);
    }

    /*
     * Handles the progress notifications received from the LSP server.
     * - Process partial results for Chat messages if provided token is maintained by ChatCommunicationManager
     * - Other notifications are ignored at this time.
     */
    @Override
    public final void notifyProgress(final ProgressParams params) {
        var chatCommunicationManager = ChatCommunicationManager.getInstance();

        ThreadingUtils.executeAsyncTask(() -> {
            try {
                chatCommunicationManager.handlePartialResultProgressNotification(params);
            } catch (Exception e) {
                Activator.getLogger().error("Error processing partial result progress notification", e);
            }
        });
    }

    @Override
    public final void telemetryEvent(final Object event) {
        TelemetryEvent telemetryEvent = ObjectMapperFactory.getInstance().convertValue(event, TelemetryEvent.class);
        ThreadingUtils.executeAsyncTask(() -> {
            switch (telemetryEvent.name()) {
            // intercept the feedback telemetry event and re-route to our feedback backend
            case "amazonq_sendFeedback":
                sendFeedback(telemetryEvent);
                break;
            default:
                Activator.getTelemetryService().emitMetric(telemetryEvent);
            }
        });
    }

    private void sendFeedback(final TelemetryEvent telemetryEvent) {
        var data = telemetryEvent.data();

        if (data.containsKey("sentiment")) {
            var sentiment = data.get("sentiment").equals(Sentiment.POSITIVE) ? Sentiment.POSITIVE : Sentiment.NEGATIVE;

            var comment = Optional.ofNullable(data.get("comment")).filter(String.class::isInstance)
                    .map(String.class::cast).orElse("");
            try {
                Activator.getTelemetryService().emitFeedback(comment, sentiment);
            } catch (Exception e) {
                Activator.getLogger().error("Error occurred when submitting feedback", e);
            }
        }
    }

    @Override
    public final CompletableFuture<ShowDocumentResult> showDocument(final ShowDocumentParams params) {
        String uri = params.getUri();
        Activator.getLogger().info("Opening URI: " + uri);

        return CompletableFuture.supplyAsync(() -> {
            final boolean[] success = new boolean[1];
                if (isLocalFile(uri)) {
                    Display.getDefault().syncExec(() -> {
                        try {
                            if (uri.endsWith("default.md")) {
                                success[0] = false;
                                Activator.getLogger().warn("Received request to open default.md - ignoring");
                                return;
                            }
                            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                            IFileStore fileStore = EFS.getLocalFileSystem().getStore(new URI(uri));
                            IDE.openEditorOnFileStore(page, fileStore);
                            success[0] = true;
                        } catch (Exception e) {
                            Activator.getLogger().error("Error in UI thread while opening URI: " + uri, e);
                            success[0] = false;
                        }
                    });
                    return new ShowDocumentResult(success[0]);
                } else {
                    Display.getDefault().syncExec(() -> {
                        try {
                            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(uri));
                            success[0] = true;
                        } catch (Exception e) {
                            Activator.getLogger().error("Error in UI thread while opening external URI: " + uri, e);
                            success[0] = false;
                        }
                    });
                    return new ShowDocumentResult(success[0]);
                }
        });
    }

    @Override
    public final void ssoTokenChanged(final SsoTokenChangedParams params) {
        SsoTokenChangedKind kind = SsoTokenChangedKind.fromValue(params.kind());
        Activator.getLogger().info("Processing " + kind + "ssoTokenChanged notification...");

        try {
            switch (kind) {
                case EXPIRED:
                    Activator.getLoginService().expire();
                    return;
                case REFRESHED:
                    boolean loginOnInvalidToken = false;
                    Activator.getLoginService().reAuthenticate(loginOnInvalidToken);
                    return;
                default:
                    Activator.getLogger().error("Error processing ssoTokenChanged notification: Unhandled kind " + kind);
            }
        } catch (IllegalArgumentException ex) {
            Activator.getLogger().error("Error processing " + kind + " ssoTokenChanged notification", ex);
        }
    }

    @Override
    public final void sendContextCommands(final Object params) {
        var command = ChatUIInboundCommand.createCommand("aws/chat/sendContextCommands", params);
        Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
    }

    @Override
    public final CompletableFuture<OpenTabResult> openTab(final OpenTabParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String requestId = UUID.randomUUID().toString();
            var command = ChatUIInboundCommand.createCommand("aws/chat/openTab", params, requestId);
            Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
            ChatAsyncResultManager manager = ChatAsyncResultManager.getInstance();
            manager.createRequestId(requestId);
            OpenTabUiResponse response;
            try {
                Object res = ChatAsyncResultManager.getInstance().getResult(requestId);
                response = ObjectMapperFactory.getInstance().convertValue(res, OpenTabUiResponse.class);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to retrieve new tab response from chat UI", e);
            } finally {
                manager.removeRequestId(requestId);
            }
            if (response.result() == null) {
                Activator.getLogger().warn("Got null tab response from UI");
                return new OpenTabResult(null);
            }
            return response.result();
        });
    }

    private boolean isLocalFile(final String uri) {
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();

            if (scheme == null || scheme.equals("file")) {
                return true;
            }

            return uri.startsWith("file:");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public final CompletableFuture<ShowSaveFileDialogResult> showSaveFileDialog(final ShowSaveFileDialogParams params) {
        CompletableFuture<ShowSaveFileDialogResult> future = new CompletableFuture<>();
        Display.getDefault().syncExec(() -> {
            String name = "export-chat.md";
            String path = "";
            try {
                URI uri = new URI(params.defaultUri());
                File file = new File(uri);
                path = file.getParent();
                name = file.getName();
            } catch (URISyntaxException e) {
                Activator.getLogger().warn("Unable to parse file path details from showSaveFileDialog params: " + e.getMessage());
            }

            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            FileDialog dialog = new FileDialog(shell, SWT.SAVE);
            dialog.setFilterExtensions(new String[] {"*.md", "*.html"});
            dialog.setFilterPath(path);
            dialog.setFileName(name);
            dialog.setFilterNames(new String[] {"Markdown Files (.md)", "HTML Files (.html)"});
            dialog.setOverwrite(true);

            String filePath = dialog.open();
            if (filePath != null) {
                future.complete(new ShowSaveFileDialogResult(filePath));
            } else {
                future.completeExceptionally(new IllegalStateException("User did not provide file path for export"));
            }
        });
        return future;
    }

    @Override
    public final CompletableFuture<SerializedChatResult> getSerializedChat(final GetSerializedChatParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String requestId = UUID.randomUUID().toString();
            var command = ChatUIInboundCommand.createCommand("aws/chat/getSerializedChat", params, requestId);
            ChatAsyncResultManager manager = ChatAsyncResultManager.getInstance();
            manager.createRequestId(requestId);
            Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
            SerializedChatResult result;
            try {
                Object res = ChatAsyncResultManager.getInstance().getResult(requestId);
                GetSerializedChatResult serializedChatResult = ObjectMapperFactory.getInstance().convertValue(res, GetSerializedChatResult.class);
                result = serializedChatResult.result();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to retrieve serialized chat from chat UI", e);
            } finally {
                manager.removeRequestId(requestId);
            }
            return result;
        });
    }

    @Override
    public final void openFileDiff(final OpenFileDiffParams params) {
        Display.getDefault().asyncExec(() -> {
            try {
                CompareConfiguration config = new CompareConfiguration();
                config.setLeftEditable(false);
                config.setRightEditable(false);
                config.setLeftLabel("Original");
                config.setRightLabel("Modified");

                // Create document contents
                String originalContent = params.originalFileContent() != null ? params.originalFileContent() : "";
                String newContent = params.fileContent() != null ? params.fileContent() : "";

                ITypedElement left = new TextCompareInput(originalContent);
                ITypedElement right = new TextCompareInput(newContent);

                // Create compare input
                CompareEditorInput input = new CompareEditorInput(config) {
                    @Override
                    protected Object prepareInput(IProgressMonitor monitor) {
                        return new DiffNode(left, right);
                    }
                };

                input.setTitle("File Comparison");
                CompareUI.openCompareEditor(input);

            } catch (Exception e) {
                Activator.getLogger().error("Failed to open comparison: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public final void sendChatUpdate(final Object params) {
        var conversationClickCommand = new ChatUIInboundCommand("aws/chat/sendChatUpdate", null, params,
                false, null);
        Activator.getEventBroker().post(ChatUIInboundCommand.class, conversationClickCommand);
    }
}
