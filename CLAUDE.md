Shruti Sinha
:tomato:  1:47 PM
https://help.eclipse.org/latest/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/ui/IPartListener2.html this partActivated or opened should give you the ability to listen to text editor changes. You can use Q or other chats for more information on how to use this

"""Skip navigation linksEclipse Platform
2025-06 (4.36)
OverviewPackageClassUseTreeDeprecatedIndexHelp
Summary: Nested | Field | Constr | MethodDetail: Field | Constr | MethodSEARCH 
Search
Package org.eclipse.ui
Interface IPartListener2
All Known Implementing Classes:
ObjectPluginAction, PartService
public interface IPartListener2
Interface for listening to part lifecycle events.
This is a replacement for IPartListener.

As of 3.5, if the implementation of this listener also implements IPageChangedListener then it will also be notified about PageChangedEvents from parts that implement IPageChangeProvider.

This interface may be implemented by clients.

See Also:
IPartService.addPartListener(IPartListener2)
Method Summary Link icon
All MethodsInstance MethodsDefault Methods
Modifier and Type
Method
Description
default void
partActivated(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been activated.
default void
partBroughtToTop(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been brought to the top.
default void
partClosed(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been closed.
default void
partDeactivated(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been deactivated.
default void
partHidden(IWorkbenchPartReference partRef)
Notifies this listener that the given part is hidden or obscured by another part.
default void
partInputChanged(IWorkbenchPartReference partRef)
Notifies this listener that the given part's input was changed.
default void
partOpened(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been opened.
default void
partVisible(IWorkbenchPartReference partRef)
Notifies this listener that the given part is visible.
Method Details Link icon
partActivated Link icon
default void partActivated(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been activated.
Parameters:
partRef - the part that was activated
See Also:
IWorkbenchPage.activate(org.eclipse.ui.IWorkbenchPart)
partBroughtToTop Link icon
default void partBroughtToTop(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been brought to the top.
These events occur when an editor is brought to the top in the editor area, or when a view is brought to the top in a page book with multiple views. They are normally only sent when a part is brought to the top programmatically (via IPerspective.bringToTop). When a part is activated by the user clicking on it, only partActivated is sent.

Parameters:
partRef - the part that was surfaced
See Also:
IWorkbenchPage.bringToTop(org.eclipse.ui.IWorkbenchPart)
partClosed Link icon
default void partClosed(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been closed.
Note that if other perspectives in the same page share the view, this notification is not sent. It is only sent when the view is being removed from the page entirely (it is being disposed).

Parameters:
partRef - the part that was closed
See Also:
IWorkbenchPage.hideView(org.eclipse.ui.IViewPart)
partDeactivated Link icon
default void partDeactivated(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been deactivated.
Parameters:
partRef - the part that was deactivated
See Also:
IWorkbenchPage.activate(org.eclipse.ui.IWorkbenchPart)
partOpened Link icon
default void partOpened(IWorkbenchPartReference partRef)
Notifies this listener that the given part has been opened.
Note that if other perspectives in the same page share the view, this notification is not sent. It is only sent when the view is being newly opened in the page (it is being created).

Parameters:
partRef - the part that was opened
See Also:
IWorkbenchPage.showView(java.lang.String)
partHidden Link icon
default void partHidden(IWorkbenchPartReference partRef)
Notifies this listener that the given part is hidden or obscured by another part.
Parameters:
partRef - the part that is hidden or obscured by another part
partVisible Link icon
default void partVisible(IWorkbenchPartReference partRef)
Notifies this listener that the given part is visible.
Parameters:
partRef - the part that is visible
partInputChanged Link icon
default void partInputChanged(IWorkbenchPartReference partRef)
Notifies this listener that the given part's input was changed.
Parameters:
partRef - the part whose input was changed

Copyright (c) 2000, 2025 Eclipse Contributors and others. All rights reserved.Guidelines for using Eclipse APIs.

 """

"""Skip to content
Navigation Menu
aws
aws-toolkit-jetbrains

Type / to search
Code
Issues
539
Pull requests
88
Discussions
Actions
Projects
1
Security
4,979
Insights
Settings
feat(amazonq): enable pinned context and rules management #5845
 Merged
rli merged 9 commits into aws:main from avi-alpert:aalpert/pinned-context  yesterday
+193 −41 
 Conversation 20
 Commits 9
 Checks 4
 Files changed 12
 
File filter 
 
0 / 12 files viewed
  4 changes: 4 additions & 0 deletions4  
.changes/next-release/feature-7e2ed4ba-b795-4c82-a99a-da610a0432c8.json
Viewed
Original file line number	Diff line number	Diff line change
@@ -0,0 +1,4 @@
{
  "type" : "feature",
  "description" : "Amazon Q Chat: Pin context items in chat and manage workspace rules"
}
   32 changes: 32 additions & 0 deletions32  
...ommunity/src/software/aws/toolkits/jetbrains/services/amazonq/webview/BrowserConnector.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -60,6 +60,8 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_LINK_CLICK
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_LIST_CONVERSATIONS
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_OPEN_TAB
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_ADD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_REMOVE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PROMPT_OPTION_ACKNOWLEDGED
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_QUICK_ACTION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_READY
@@ -73,6 +75,7 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.Encry
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.GET_SERIALIZED_CHAT_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.GetSerializedChatResponse
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.LIST_MCP_SERVERS_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.LIST_RULES_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.MCP_SERVER_CLICK_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.OPEN_SETTINGS
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.OPEN_WORKSPACE_SETTINGS_KEY
@@ -83,6 +86,7 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.OpenT
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.OpenTabResultSuccess
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.PROMPT_INPUT_OPTIONS_CHANGE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.QuickChatActionRequest
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.RULE_CLICK_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.SEND_CHAT_COMMAND_PROMPT
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.STOP_CHAT_RESPONSE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.SendChatPromptRequest
@@ -485,6 +489,34 @@ class BrowserConnector(
                        )
                    }
            }
            LIST_RULES_REQUEST_METHOD -> {
                handleChat(AmazonQChatServer.listRules, node)
                    .whenComplete { response, _ ->
                        browser.postChat(
                            FlareUiMessage(
                                command = LIST_RULES_REQUEST_METHOD,
                                params = response
                            )
                        )
                    }
            }
            RULE_CLICK_REQUEST_METHOD -> {
                handleChat(AmazonQChatServer.ruleClick, node)
                    .whenComplete { response, _ ->
                        browser.postChat(
                            FlareUiMessage(
                                command = RULE_CLICK_REQUEST_METHOD,
                                params = response
                            )
                        )
                    }
            }
            CHAT_PINNED_CONTEXT_ADD -> {
                handleChat(AmazonQChatServer.pinnedContextAdd, node)
            }
            CHAT_PINNED_CONTEXT_REMOVE -> {
                handleChat(AmazonQChatServer.pinnedContextRemove, node)
            }
        }
    }

  53 changes: 23 additions & 30 deletions53  
plugins/amazonq/mynah-ui/package-lock.json
Viewed
Some generated files are not rendered by default. Learn more about how customized files appear on GitHub.

   4 changes: 2 additions & 2 deletions4  
plugins/amazonq/mynah-ui/package.json
Viewed
Original file line number	Diff line number	Diff line change
@@ -12,7 +12,7 @@
        "lintfix": "eslint -c .eslintrc.js --fix --ext .ts ."
    },
    "dependencies": {
        "@aws/mynah-ui-chat": "npm:@aws/mynah-ui@4.30.3",
        "@aws/mynah-ui-chat": "npm:@aws/mynah-ui@4.35.5",
samgst-amazon marked this conversation as resolved.
        "@types/node": "^14.18.5",
        "fs-extra": "^10.0.1",
        "sanitize-html": "^2.12.1",
@@ -21,8 +21,8 @@
        "web-tree-sitter": "^0.20.7"
    },
    "devDependencies": {
        "@aws/chat-client": "^0.1.18",
        "@aws/fully-qualified-names": "^2.1.1",
        "@aws/chat-client": "^0.1.4",
        "@types/sanitize-html": "^2.8.0",
        "@typescript-eslint/eslint-plugin": "^5.38.0",
        "@typescript-eslint/parser": "^5.38.0",
   2 changes: 1 addition & 1 deletion2  
plugins/amazonq/mynah-ui/src/mynah-ui/ui/quickActions/handler.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -395,7 +395,7 @@ private handleDocCommand(chatPrompt: ChatPrompt, tabID: string, taskName: string
                cancelButtonWhenLoading: false,
            })
        } else {
            this.mynahUI?.updateStore(affectedTabId, { promptInputOptions: [] })
            this.mynahUI?.updateStore(affectedTabId, { promptInputOptions: [], promptTopBarTitle: '' })
        }

        if (affectedTabId && this.isHybridChatEnabled) {
   26 changes: 26 additions & 0 deletions26  
...s-community/src/software/aws/toolkits/jetbrains/services/amazonq/lsp/AmazonQChatServer.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -18,6 +18,8 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_INSERT_TO_CURSOR_NOTIFICATION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_LINK_CLICK
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_LIST_CONVERSATIONS
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_ADD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_REMOVE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_QUICK_ACTION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_READY
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SOURCE_LINK_CLICK
@@ -39,11 +41,13 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.GetSe
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.InfoLinkClickParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.InsertToCursorPositionParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.LIST_MCP_SERVERS_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.LIST_RULES_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.LinkClickParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.ListConversationsParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.MCP_SERVER_CLICK_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.PROMPT_INPUT_OPTIONS_CHANGE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.PromptInputOptionChangeParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.RULE_CLICK_REQUEST_METHOD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.SEND_CHAT_COMMAND_PROMPT
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.SourceLinkClickParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.TELEMETRY_EVENT
@@ -189,6 +193,18 @@ object AmazonQChatServer : JsonRpcMethodProvider {
        LSPAny::class.java
    )

    val listRules = JsonRpcRequest(
        LIST_RULES_REQUEST_METHOD,
        LSPAny::class.java,
        LSPAny::class.java
    )

    val ruleClick = JsonRpcRequest(
        RULE_CLICK_REQUEST_METHOD,
        LSPAny::class.java,
        LSPAny::class.java
    )

    val conversationClick = JsonRpcRequest(
        CHAT_CONVERSATION_CLICK,
        ConversationClickParams::class.java,
@@ -218,6 +234,16 @@ object AmazonQChatServer : JsonRpcMethodProvider {
        CreatePromptParams::class.java
    )

    val pinnedContextAdd = JsonRpcNotification(
        CHAT_PINNED_CONTEXT_ADD,
        LSPAny::class.java
    )

    val pinnedContextRemove = JsonRpcNotification(
        CHAT_PINNED_CONTEXT_REMOVE,
        LSPAny::class.java
    )

    val telemetryEvent = JsonRpcNotification(
        TELEMETRY_EVENT,
        Any::class.java
   12 changes: 12 additions & 0 deletions12  
...mmunity/src/software/aws/toolkits/jetbrains/services/amazonq/lsp/AmazonQLanguageClient.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -9,7 +9,10 @@ import org.eclipse.lsp4j.services.LanguageClient
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.LSPAny
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_OPEN_TAB
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_OPTIONS_UPDATE_NOTIFICATION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_ADD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_REMOVE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_CONTEXT_COMMANDS
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_PINNED_CONTEXT
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_UPDATE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CopyFileParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.DID_APPEND_FILE
@@ -54,6 +57,15 @@ interface AmazonQLanguageClient : LanguageClient {
    @JsonNotification(CHAT_SEND_CONTEXT_COMMANDS)
    fun sendContextCommands(params: LSPAny): CompletableFuture<Unit>

    @JsonNotification(CHAT_SEND_PINNED_CONTEXT)
    fun sendPinnedContext(params: LSPAny)

    @JsonNotification(CHAT_PINNED_CONTEXT_ADD)
    fun pinnedContextAdd(params: LSPAny)

    @JsonNotification(CHAT_PINNED_CONTEXT_REMOVE)
    fun pinnedContextRemove(params: LSPAny)

    @JsonNotification(DID_COPY_FILE)
    fun copyFile(params: CopyFileParams)

   64 changes: 57 additions & 7 deletions64  
...ity/src/software/aws/toolkits/jetbrains/services/amazonq/lsp/AmazonQLanguageClientImpl.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -16,6 +16,7 @@ import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import migration.software.aws.toolkits.jetbrains.settings.AwsSettings
import org.eclipse.lsp4j.ConfigurationParams
@@ -27,6 +28,7 @@ import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowDocumentParams
import org.eclipse.lsp4j.ShowDocumentResult
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
@@ -44,7 +46,10 @@ import software.aws.toolkits.jetbrains.services.amazonq.lsp.flareChat.FlareUiMes
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.LSPAny
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_OPEN_TAB
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_OPTIONS_UPDATE_NOTIFICATION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_ADD
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_PINNED_CONTEXT_REMOVE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_CONTEXT_COMMANDS
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_PINNED_CONTEXT
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CHAT_SEND_UPDATE
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.CopyFileParams
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.FileParams
@@ -74,7 +79,8 @@ import java.util.concurrent.TimeUnit
 * Concrete implementation of [AmazonQLanguageClient] to handle messages sent from server
 */
class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageClient {

    private val chatManager
        get() = ChatCommunicationManager.getInstance(project)
    private fun handleTelemetryMap(telemetryMap: Map<*, *>) {
        try {
            val name = telemetryMap["name"] as? String ?: return
@@ -201,7 +207,6 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC
    override fun openTab(params: LSPAny): CompletableFuture<LSPAny> {
        val requestId = UUID.randomUUID().toString()
        val result = CompletableFuture<LSPAny>()
        val chatManager = ChatCommunicationManager.getInstance(project)
        chatManager.addTabOpenRequest(requestId, result)

        chatManager.notifyUi(
@@ -252,7 +257,6 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC
    override fun getSerializedChat(params: LSPAny): CompletableFuture<GetSerializedChatResult> {
        val requestId = UUID.randomUUID().toString()
        val result = CompletableFuture<GetSerializedChatResult>()
        val chatManager = ChatCommunicationManager.getInstance(project)
        chatManager.addSerializedChatRequest(requestId, result)

        chatManager.notifyUi(
@@ -317,9 +321,8 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC

    override fun notifyProgress(params: ProgressParams?) {
        if (params == null) return
        val chatCommunicationManager = ChatCommunicationManager.getInstance(project)
        try {
            chatCommunicationManager.handlePartialResultProgressNotification(project, params)
            chatManager.handlePartialResultProgressNotification(project, params)
        } catch (e: Exception) {
            LOG.error(e) { "Cannot handle partial chat" }
        }
@@ -415,7 +418,6 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC
        )

    override fun sendContextCommands(params: LSPAny): CompletableFuture<Unit> {
        val chatManager = ChatCommunicationManager.getInstance(project)
        chatManager.notifyUi(
            FlareUiMessage(
                command = CHAT_SEND_CONTEXT_COMMANDS,
@@ -425,6 +427,55 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC
        return CompletableFuture.completedFuture(Unit)
    }

    override fun sendPinnedContext(params: LSPAny) {
        // Send the active text file path with pinned context
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
Member
@manodnyab manodnyab 5 days ago
Would recommend creating utils for this since the same util is being used while sending chat prompts in LspEditorUtils

Member
Author
@avi-alpert avi-alpert 5 days ago
this behaves differently from when we send chat prompt, since here we want to send the relative path to the active file. the only common call is FileEditorManager.getInstance(project).selectedTextEditor

Member
@rli rli 2 days ago
we will eventually need to ban selectedTextEditor but that can be done later

@aseemxs	Reply...
        val textDocument = editor?.let {
            val relativePath = VfsUtilCore.getRelativePath(it.virtualFile, project.baseDir)
                ?: it.virtualFile.path // Use absolute path if not in project
            TextDocumentIdentifier(relativePath)
        }

        // Create updated params with text document information
        // Since params is LSPAny, we need to handle it as a generic object
        val updatedParams = when (params) {
            is Map<*, *> -> {
                val mutableParams = params.toMutableMap()
                mutableParams["textDocument"] = textDocument
                mutableParams
            }
            else -> mapOf(
Member
@rli rli 2 days ago
will it ever hit this case or is it always a map?

@aseemxs	Reply...
                "params" to params,
                "textDocument" to textDocument
            )
        }

        chatManager.notifyUi(
            FlareUiMessage(
                command = CHAT_SEND_PINNED_CONTEXT,
                params = updatedParams,
            )
        )
    }

    override fun pinnedContextAdd(params: LSPAny) {
        chatManager.notifyUi(
            FlareUiMessage(
                command = CHAT_PINNED_CONTEXT_ADD,
                params = params,
            )
        )
    }

    override fun pinnedContextRemove(params: LSPAny) {
        chatManager.notifyUi(
            FlareUiMessage(
                command = CHAT_PINNED_CONTEXT_REMOVE,
                params = params,
            )
        )
    }

    override fun appendFile(params: FileParams) = refreshVfs(params.path)

    override fun createDirectory(params: FileParams) = refreshVfs(params.path)
@@ -439,7 +490,6 @@ class AmazonQLanguageClientImpl(private val project: Project) : AmazonQLanguageC
    }

    override fun sendChatOptionsUpdate(params: LSPAny) {
        val chatManager = ChatCommunicationManager.getInstance(project)
        chatManager.notifyUi(
            FlareUiMessage(
                command = CHAT_OPTIONS_UPDATE_NOTIFICATION,
   4 changes: 3 additions & 1 deletion4  
.../src/software/aws/toolkits/jetbrains/services/amazonq/lsp/model/ExtendedClientMetadata.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -24,6 +24,7 @@ data class AwsClientCapabilities(
data class DeveloperProfiles(
    val developerProfiles: Boolean,
    val mcp: Boolean,
    val pinnedContextEnabled: Boolean,
)

data class WindowSettings(
@@ -62,7 +63,8 @@ fun createExtendedClientMetadata(project: Project): ExtendedClientMetadata {
            awsClientCapabilities = AwsClientCapabilities(
                q = DeveloperProfiles(
                    developerProfiles = true,
                    mcp = true
                    mcp = true,
                    pinnedContextEnabled = true,
                ),
                window = WindowSettings(
                    showSaveFileDialog = true
   1 change: 1 addition & 0 deletions1  
...software/aws/toolkits/jetbrains/services/amazonq/lsp/model/aws/chat/CreatePromptParams.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -5,6 +5,7 @@ package software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat

data class CreatePromptParams(
    val promptName: String,
    val isRule: Boolean? = null,
)

data class CreatePromptNotification(
   6 changes: 6 additions & 0 deletions6  
.../software/aws/toolkits/jetbrains/services/amazonq/lsp/model/aws/chat/FlareChatCommands.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -3,6 +3,7 @@

package software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat

const val ACTIVE_EDITOR_CHANGED_NOTIFICATION = "aws/chat/activeEditorChanged"
const val AUTH_FOLLOW_UP_CLICKED = "authFollowUpClicked"
const val CHAT_BUTTON_CLICK = "aws/chat/buttonClick"
const val CHAT_CONVERSATION_CLICK = "aws/chat/conversationClick"
@@ -25,7 +26,10 @@ const val CHAT_PROMPT_OPTION_ACKNOWLEDGED = "chatPromptOptionAcknowledged"
const val CHAT_QUICK_ACTION = "aws/chat/sendChatQuickAction"
const val CHAT_READY = "aws/chat/ready"
const val CHAT_SEND_CONTEXT_COMMANDS = "aws/chat/sendContextCommands"
const val CHAT_SEND_PINNED_CONTEXT = "aws/chat/sendPinnedContext"
const val CHAT_SEND_UPDATE = "aws/chat/sendChatUpdate"
const val CHAT_PINNED_CONTEXT_ADD = "aws/chat/pinnedContextAdd"
const val CHAT_PINNED_CONTEXT_REMOVE = "aws/chat/pinnedContextRemove"
const val CHAT_SOURCE_LINK_CLICK = "aws/chat/sourceLinkClick"
const val CHAT_TAB_ADD = "aws/chat/tabAdd"
const val CHAT_TAB_BAR_ACTIONS = "aws/chat/tabBarAction"
@@ -55,3 +59,5 @@ const val TELEMETRY_EVENT = "telemetry/event"
// https://github.com/aws/language-server-runtimes/blob/112feba70219a98a12f13727d67c540205fa9c9f/types/chat.ts#L32
const val LIST_MCP_SERVERS_REQUEST_METHOD = "aws/chat/listMcpServers"
const val MCP_SERVER_CLICK_REQUEST_METHOD = "aws/chat/mcpServerClick"
const val LIST_RULES_REQUEST_METHOD = "aws/chat/listRules"
const val RULE_CLICK_REQUEST_METHOD = "aws/chat/ruleClick"
   26 changes: 26 additions & 0 deletions26  
...re/aws/toolkits/jetbrains/services/amazonq/lsp/textdocument/TextDocumentServiceHandler.kt
Viewed
Original file line number	Diff line number	Diff line change
@@ -10,8 +10,11 @@ import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
@@ -28,6 +31,8 @@ import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import software.aws.toolkits.jetbrains.services.amazonq.lsp.AmazonQLspService
import software.aws.toolkits.jetbrains.services.amazonq.lsp.model.aws.chat.ACTIVE_EDITOR_CHANGED_NOTIFICATION
import software.aws.toolkits.jetbrains.services.amazonq.lsp.util.LspEditorUtil.getCursorState
import software.aws.toolkits.jetbrains.services.amazonq.lsp.util.LspEditorUtil.toUriString
import software.aws.toolkits.jetbrains.utils.pluginAwareExecuteOnPooledThread

@@ -166,6 +171,27 @@ class TextDocumentServiceHandler(
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        handleActiveEditorChange(event.newEditor)
    }

    private fun handleActiveEditorChange(fileEditor: FileEditor?) {
        // Extract text editor if it's a TextEditor, otherwise null
        val editor = (fileEditor as? TextEditor)?.editor
        val textDocumentIdentifier = editor?.let { TextDocumentIdentifier(toUriString(it.virtualFile)) }
        val cursorState = editor?.let { getCursorState(it) }

        val params = mapOf(
            "textDocument" to textDocumentIdentifier,
            "cursorState" to cursorState
        )

        // Send notification to the language server
        AmazonQLspService.executeIfRunning(project) { _ ->
            rawEndpoint.notify(ACTIVE_EDITOR_CHANGED_NOTIFICATION, params)
        }
    }

    private fun realTimeEdit(event: DocumentEvent) {
        AmazonQLspService.executeIfRunning(project) { languageServer ->
            pluginAwareExecuteOnPooledThread {
Footer
© 2025 GitHub, Inc.
Footer navigation
Terms
Privacy
Security
Status
Docs
Contact
Manage cookies
Do not share my personal information
 """

"""VS: Pinned context

What is it

* The chat UI introduces “pills” UX. These are pinned context items displayed in a row at the top of the chat input box, as shown in the mockup below. As mentioned earlier, these pills will be used to display context items either “pinned” by the user or implicitly added by Q, and which will be injected automatically into every chat message.



* Pinned context items only apply to the current conversation, i.e., tab. If the user opens a new tab, do not carry over any pinned items from the current tab (other than any items we add by default, e.g., the current file).
* Pinned context items are prime candidates for prompt caching. The user has explicitly told us that they will be using these items across multiple messages, so we should cache them to save on encoding latency and cost.
* Add pinned itemsby dragging and dropping from context menu, prompt input area, ide project file view, editor view, os filesystem
* Removing pinned items by either clicking the “x” on the right or draggingand dropping a pill outside of the area




How do we add it to VS

* Extension sets an pinnedContextEnabled flag as part of the clientCapabilities in the initialization handshake
* Flare does not need to send back anything for Extension in ChatOptions
* chat-client only tells mynahui to display pinned context if it gets a sendPinnedContext notification from the server. And the server only sends a sendPinnedContext notification to chat-client if it received pinnedContextEnabled: true in initialization options
* gets sendPinnedContext notification from server 
* aws/chat/activeEditorChanged - Client to Server notification when active editor changes
    * we are sending back textDocument and CursorState, there might be a listener for that during the initialization
* aws/chat/pinnedContextAdd - Client to Server notification when pinned context is added 
* aws/chat/pinnedContextRemove - Client to Server notification when pinned context is removed 
* activeEditorChanges, Add, and remove send sendPinnedContext 
* aws/chat/listRules - Client to Server request to list available rules for a specific tab
* aws/chat/ruleClick - Client to Server request for rule or rule folder click events



Implementation

For the activeEditorChanged, we need to have a way to listen the tab/window changed in VS. We use GotAggregateFocus api for this tracking. 
Limitations: This implementation may fire more frequently than VS Code/JetBrains equivalents since GotAggregateFocus
 triggers on any focus event (clicking within editor, returning from dialogs, etc.), not just tab switches. It tracks all text content types, potentially including non-document views like output windows. However, these extra events won't break pinned context since they send the same file path and cursor position for the active document, and the LSP server can deduplicate identical notifications..
Why It's Better Than Alternatives: This approach reliably detects editor focus changes rather than just document opens, making it superior to RunningDocumentTableEvents for tab switching. Unlike DTE  WindowEvents, it uses lightweight VS SDK events with automatic MEF lifecycle management and no UI thread constraints. It provides direct cursor access, follows VS extension patterns correctly, and ensures the language server stays synchronized with the true active editor state through comprehensive focus tracking.




Effort Estimates

* pending confirmation that major pieces of scope are not missing from above

Pending Issues

* UI is a a problem for pinned context

⏹️ You can stop reading here ⏹️

Raw Notes

* references
    * Pinned Context overview Falcon Product requirements
    * Figma
    * VS code Bug Bash Pinned Context Bug Bash - VS Code
* VSC and language commits:
    * https://github.com/aws/mynah-ui/pull/336
    * https://github.com/aws/language-server-runtimes/pull/548
    * https://github.com/aws/language-servers/pull/1663
    * https://github.com/aws/aws-toolkit-vscode/pull/7493
* JetBrains PR: https://github.com/aws/aws-toolkit-jetbrains/pull/5845

 """
"""Pinned Context Bug Bash - VS Code

Most recent build uploaded: 6/16 7:20PM eastern


New installation instructions

* Download the amazon-q-vscode vsix located at https://github.com/aws/aws-toolkit-vscode/releases/tag/pre-LSP-alpha
* Install it in VS Code, and restart IDE



How to install (DEPRECATED)

1. Download these two .js files and put them in the root of your home directory (“~/”).
    1. https://drive.corp.amazon.com/documents/aalpert@/Shared/pinned%20context/june%2016/amazonq-ui.js
    2. https://drive.corp.amazon.com/documents/aalpert@/Shared/pinned%20context/june%2016/aws-lsp-codewhisperer.js
2. ONE TIME ONLY: To get local indexing to work, you will need to download the indexing folder for your OS:
    1. If you have a Mac with an M1 or later chip, download servers.zip, and skip to step c.
    2. If you have a different OS, go to manifest and download the servers.zip for your OS and architecture
    3. Unzip servers.zip and copy the indexing folder to the root of your home directory. the indexing folder should now live at ~/indexing. 
    4. Your home directory should now have: amazonq-ui.js, aws-lsp-codewhisperer.js, and the indexing folder.
    5. Mac OS only: Your Mac security settings will likely refuse to open the .node files in this folder. To allow running these files, run xattr -dr com.apple.quarantine ~/indexing in Terminal
3. Download the VSIX and install it in vs code:
    1. https://drive.corp.amazon.com/documents/aalpert@/Shared/pinned%20context/june%2016/amazon-q-vscode-1.76.0-gd7af62c.vsix
    2. Run Extensions: Install from VSIX... from the VSCode command palette and choose the vsix file.
    3. Make sure you do not have the aws.experiments overrides on in your VS Code user settings
4. Restart vs code.



Test cases

Note: Before reading through the test cases, try playing with the features first to see how intuitive/easy it is. UX you find confusing or counterintuitive should be reported in table below.

Pinned Context

1. To pin a context item, Click @Pin Context and select an item to pin it (click or Enter key). @Pin Context should be shortened to @ once you have added an item
2. To remove a pinned context item, click an item in pinned context area. Hovering over a pinned context pill should switch icon to an X and show tooltip with path to the item.
3. Type @ and pin a context item by keying ⌥ + Enter (item with active keyboard selection should be pinned)
4. Type @ and add context item to prompt (mouse click or Enter key), then click pill in prompt to pin it. Hovering over a context pill in the prompt should switch icon to a Pin and show tooltip with path to the item.
5. Active file pin should show up by default if there is an active text file open in IDE, it should disappear if there is no active text file (i.e. when all text files are closed), and reappear again if an active file is opened. 
6. If active file pill is manually removed by user, it should never reappear in the conversation. To bring it back after manually removing it, choose Active file option in context list. 
7. Hover over active file pill and you should see the path to the file thats active. if active file in IDE is switched, hovering over pill again should show most up to date active file.
8. Pinned context overflow: Shrink and expand width of Amazon Q extension, overflow pill should show +N, where N is number of pinned items that are overflowed. Click on item in overflow to remove it (X icon on hover)
9. Pinning an item that has already been pinned has no affect - there should be no duplicates
10. Send a message with pinned context, make sure pinned context items are shown in context transparency list at top of the response. Pinned context items should be persisted within a tab.
11. Close a tab and reopen it from history, pinned context should be restored.
12. Quit IDE and reopen it, previously open tabs should reopen with their pinned context restored.

Rules

1. Click Rules button and click create a rule, it should save .md file with specified name in .amazonq/rules
2. Toggle on/off rules (or folders of rules) from rules list. Rules should be checked by default, and are grouped by folders. The rules list shows all .md files that live in workspace root’s .amazonq/rules (rules can be nested in folders inside this directory).
3. The AmazonQ.md living in the workspace root should be automatically included as a rule and sent with every prompt, unless user unchecks them from Rules list
4. Send a message to Q and make sure that rules that are checked show up in context transparency list and are reflected in the response.
5. Exit conversation and reopen it from history, rules state (whether they are checked or unchecked) should be restored. Rules state is scoped to the current tab. Opening a new tab should have all rules checked by default.
6. After creating a rule file, list should automatically update. You can try creating one in Finder and it should appear in IDE when you click Rules button. When deleting a rule from filesystem, it should disappear from list.
7. If a rule folder is unchecked, then if a new rule is added to that folder, it should be unchecked by default (i.e. new rules inherit the state from parent folder). Otherwise, all new rules should be checked by default.
8. In a multi root workspace, rules from all workspace roots should appear. Folder paths should include the workspace root name only if its a multi-root workspace.
9. Quit IDE and reopen, previously open tabs should reopen with their previous rules state restored.
10. If you do not have an open workspace, Rules button should be hidden



Bugs/Feedback

Reporter	Date Reported	Feature	Bug name	Priority (do not change)	Status	Notes	Screenshot
Avi Alpert	6/5	Rules	when creating a rule, if inputted name is invalid, the Create button greys out but the validation error only shows up if you click Create.	P1	Not Started	Existing bug in prod	
Avi Alpert	6/6	pinned	Add a code symbol to pinned context. Exit IDE, reopen it. if you send a message before indexing finishes, it wont actually send the symbol	P1	Not Started		
Arun			Active file option should be inside of Files list, not at root of context list	P1	In Progress		
Avi Alpert	6/5	pinned	Active file does not show up in context transparency list	P1 - high	In Progress	Existing bug in prod	
Avi Alpert	6/5	pinned	Properly handle pinned context items that have been deleted. Arun: if a pinned context item's underlying file was deleted, we should add a visual indicator that it was deleted. for v1 its ok to just keep it there, but at minimum make sure we dont throw an error.	P2	Not Started	Currently no error is thrown	
Avi Alpert	6/5	Rules	Can't use keyboard to iterate through list, its mouse only	P2	Not Started		
Avi Alpert	6/6	pinned	When I click @ button to pin context, search is autofocused initially but if i click a group like Folders, Files,etc, search gets unfocused. search input should stay foused the entire time this overlay is open	P2	Not Started		
Avi Alpert	6/6	pinned	When there is no active text file, I can still see the active file option in menu. but trying to pin it does nothing	P2	Not Started	Should we hide Active file option from list if there is no active file open?

[We should gray it out - Arun.]	
Arun			There is no visual differentiator between pinned context area and prompt area	P2	Not Started	follow up with UX	
Avi Alpert	6/5	Rules	toggling a rule causes row heights to slightly jump, its unsettling	P2	Not Started		
Avi Alpert	6/5	pinned	pinning @sage has no effect. this is because backend looks for @sage in the prompt itself	P2	Not Started	What is solution? Manually adding @sage in every message?	
Jingyuan Li	6/6	context in prompt	Minor UI thing, the context box gets cut-off when we have multiple lines in chat box	P2	Not Started		
Yuxian Zhang	6/6	context in prompt	When creating a new prompt, if I input nothing in the prompt name box and click the X on the rightup, it shows warning message instead of collapsing the interface	P2	Not Started		
Arun Nair	6/6		Sometimes when I create a new tab, it uses the pinned context from the current tab. I am not able to repro this consistently though.	P2	Not Started		
Arun Nair	6/6		I like the search functionality when users click on the "@" menu. We should keep this functionality when users press the "@" key as well, as some users said that it's not obvious that they could just keep typing to search.	P2	Not Started		
Yang Zhang	6/6		When open a new chat tab and remove one of the pinned context, all items are deleted	P2	Not Started	This is a side effect of the bug #17	
Arun Nair	6/6		Sometimes we show the [+1] thing even if there was space to show more items. (I think this happens at startup, probably because we're measuring width while the layout is still not complete.)	P2	Not Started		
Ryan Dagley	6/11/2025	rules	Creating a new rule with the same name as an existing rule will overwrite the existing rule.  The user is not informed.	P2	Not Started		
Jingyuan Li	6/25	pinned context?	In a chat tab with messages, do /review, it pops up a new /review window with pinned context. Pinned context is not supposed to be in quick action tabs	P2	Not Started	We will replace the quick actions with agents soon. We may not want to spend time on this since this would be throw-away work	












Fixed Bugs

Reporter	Date	Feature	Bug name	Priority	Status	Notes	Screenshot
Avi	6/3/2025		When clicking @ button with mouse and selecting an item it should be added to Pinned context. When typing @ and selecting an item (click or enter), it should be added to prompt. When typing @ and keying Option-Enter, it should be added to pinned context.

The bug in the current build is that if you open the menu through clicking @, select an item, and then open the menu again by typing @ (or vice versa), it adds the context based on how you first opened the menu, instead of how you just opened it	P0	Complete	Fixed in build uploaded at 6/3 6:58PM eastern	
Avi	6/3/2025		When the IDE is opened with no workspace, the Rules button should not be visible	P0	Complete	Fixed on 6/4 build	
Avi	6/3/2025		Hovering over pinned @workspace doesnt show an X icon	P0	Complete	Desired behavior: in the case of a  context pill doesnt have an icon (like @workspace), the asterisk should be replaced by the X or pin

Fixed on 6/4 build	
Avi	6/3/2025		When there is no active file open, the active file tab should not be visible in pinned context	P0	Complete	Fixed on 6/4 build	
Avi	6/3/2025		The @ button in the bottom right of prompt area should not be there anymore	P0	Complete	Fixed on 6/4 build	
Avi	6/3/2025		Hovering over 'active file' context pill shows absolute path. desired UX: if file is not in an open workspace, show absolute path. otherwise, show relative path, like for other pinned context files	P0	Complete		
-	6/3/2025		Keyboard navigation/filtering does not work after click @ button with mouse	P0	Complete		
-	6/3/2025		Escape key doesn't close overlay after click @ button with mouse	P0	Complete		
Jingyuan	6/3/2025		After clicking on the context to pin, should it be removed from the chat box?		Cancelled		
Jingyuan	6/3/2025		The pined context is showing up in /review panel as well.	P0	Complete	Fixed on 6/4 build	
Avi	6/4/2025		There should be a visible hint to key Option Enter to pin (only in overlay that pops up after keying @)	P0	Complete		
Avi	6/5/2025		The hovered icon state sometimes persist - it should be CSS based using :hover	P0	Complete		
Avi Alpert	6/3	pinned	When hovering over a context pill in the prompt area, the icon should be a Pin, not a paper clip	P0	Complete	Waiting for Zakiya to provide SVG to add to mynah ui	
							
							
							
							
							

 """

""" Skip to content
Navigation Menu
aws
aws-toolkit-vscode

Type / to search
Code
Issues
506
Pull requests
109
Discussions
Actions
Security
94
Insights
Settings
feat(amazonq): support pinned context #7493
 Merged
avi-alpert merged 2 commits into aws:master from avi-alpert:aalpert/pinned-context  last week
+454 −47 
 Conversation 1
 Commits 2
 Checks 25
 Files changed 7
 Merged
feat(amazonq): support pinned context
#7493
 
File filter 
 
0 / 7 files viewed
  439 changes: 398 additions & 41 deletions439  
package-lock.json
Viewed
Large diffs are not rendered by default.

   7 changes: 6 additions & 1 deletion7  
packages/amazonq/src/lsp/chat/activation.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -7,7 +7,11 @@ import { window } from 'vscode'
import { LanguageClient } from 'vscode-languageclient'
import { AmazonQChatViewProvider } from './webviewProvider'
import { focusAmazonQPanel, registerCommands } from './commands'
import { registerLanguageServerEventListener, registerMessageListeners } from './messages'
import {
    registerActiveEditorChangeListener,
    registerLanguageServerEventListener,
    registerMessageListeners,
} from './messages'
import { Commands, getLogger, globals, undefinedIfEmpty } from 'aws-core-vscode/shared'
import { activate as registerLegacyChatListeners } from '../../app/chat/activation'
import { DefaultAmazonQAppInitContext } from 'aws-core-vscode/amazonq'
@@ -33,6 +37,7 @@ export async function activate(languageClient: LanguageClient, encryptionKey: Bu
     **/
    registerCommands(provider)
    registerLanguageServerEventListener(languageClient, provider)
    registerActiveEditorChangeListener(languageClient)

    provider.onDidResolveWebview(() => {
        const disposable = DefaultAmazonQAppInitContext.instance.getAppsToWebViewMessageListener().onMessage((msg) => {
   45 changes: 44 additions & 1 deletion45  
packages/amazonq/src/lsp/chat/messages.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -55,14 +55,18 @@ import {
    ChatUpdateParams,
    chatOptionsUpdateType,
    ChatOptionsUpdateParams,
    listRulesRequestType,
    ruleClickRequestType,
    pinnedContextNotificationType,
    activeEditorChangedNotificationType,
} from '@aws/language-server-runtimes/protocol'
import { v4 as uuidv4 } from 'uuid'
import * as vscode from 'vscode'
import { Disposable, LanguageClient, Position, TextDocumentIdentifier } from 'vscode-languageclient'
import * as jose from 'jose'
import { AmazonQChatViewProvider } from './webviewProvider'
import { AuthUtil, ReferenceLogViewProvider } from 'aws-core-vscode/codewhisperer'
import { amazonQDiffScheme, AmazonQPromptSettings, messages, openUrl } from 'aws-core-vscode/shared'
import { amazonQDiffScheme, AmazonQPromptSettings, messages, openUrl, isTextEditor } from 'aws-core-vscode/shared'
import {
    DefaultAmazonQAppInitContext,
    messageDispatcher,
@@ -74,6 +78,29 @@ import { telemetry, TelemetryBase } from 'aws-core-vscode/telemetry'
import { isValidResponseError } from './error'
import { focusAmazonQPanel } from './commands'

export function registerActiveEditorChangeListener(languageClient: LanguageClient) {
    let debounceTimer: NodeJS.Timeout | undefined
    vscode.window.onDidChangeActiveTextEditor((editor) => {
        if (debounceTimer) {
            clearTimeout(debounceTimer)
        }
        debounceTimer = setTimeout(() => {
            let textDocument = undefined
            let cursorState = undefined
            if (editor) {
                textDocument = {
                    uri: editor.document.uri.toString(),
                }
                cursorState = getCursorState(editor.selections)
            }
            languageClient.sendNotification(activeEditorChangedNotificationType.method, {
                textDocument,
                cursorState,
            })
        }, 100)
    })
}

export function registerLanguageServerEventListener(languageClient: LanguageClient, provider: AmazonQChatViewProvider) {
    languageClient.info(
        'Language client received initializeResult from server:',
@@ -316,6 +343,8 @@ export function registerMessageListeners(
                )
                break
            }
            case listRulesRequestType.method:
            case ruleClickRequestType.method:
            case listConversationsRequestType.method:
            case conversationClickRequestType.method:
            case listMcpServersRequestType.method:
@@ -471,6 +500,20 @@ export function registerMessageListeners(
            params: params,
        })
    })
    languageClient.onNotification(
        pinnedContextNotificationType.method,
        (params: ContextCommandParams & { tabId: string; textDocument?: TextDocumentIdentifier }) => {
            const editor = vscode.window.activeTextEditor
            let textDocument = undefined
            if (editor && isTextEditor(editor)) {
                textDocument = { uri: vscode.workspace.asRelativePath(editor.document.uri) }
            }
            void provider.webview?.postMessage({
                command: pinnedContextNotificationType.method,
                params: { ...params, textDocument },
            })
        }
    )

    languageClient.onNotification(openFileDiffNotificationType.method, async (params: OpenFileDiffParams) => {
        const ecc = new EditorContentController()
   1 change: 1 addition & 0 deletions1  
packages/amazonq/src/lsp/client.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -123,6 +123,7 @@ export async function startLanguageServer(
                awsClientCapabilities: {
                    q: {
                        developerProfiles: true,
                        pinnedContextEnabled: true,
                        mcp: true,
                    },
                    window: {
   6 changes: 3 additions & 3 deletions6  
packages/core/package.json
Viewed
Original file line number	Diff line number	Diff line change
@@ -443,8 +443,8 @@
        "@aws-sdk/types": "^3.13.1",
        "@aws/chat-client": "^0.1.4",
        "@aws/chat-client-ui-types": "^0.1.24",
        "@aws/language-server-runtimes": "^0.2.81",
        "@aws/language-server-runtimes-types": "^0.1.28",
        "@aws/language-server-runtimes": "^0.2.97",
        "@aws/language-server-runtimes-types": "^0.1.39",
        "@cspotcode/source-map-support": "^0.8.1",
        "@sinonjs/fake-timers": "^10.0.2",
        "@types/adm-zip": "^0.4.34",
@@ -526,7 +526,7 @@
        "@aws-sdk/s3-request-presigner": "<3.731.0",
        "@aws-sdk/smithy-client": "<3.731.0",
        "@aws-sdk/util-arn-parser": "<3.731.0",
        "@aws/mynah-ui": "^4.34.1",
        "@aws/mynah-ui": "^4.35.4",
        "@gerhobbelt/gitignore-parser": "^0.2.0-9",
        "@iarna/toml": "^2.2.5",
        "@smithy/fetch-http-handler": "^5.0.1",
   2 changes: 1 addition & 1 deletion2  
packages/core/src/amazonq/webview/ui/quickActions/handler.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -362,7 +362,7 @@ export class QuickActionHandler {
                cancelButtonWhenLoading: false,
            })
        } else {
            this.mynahUI.updateStore(affectedTabId, { promptInputOptions: [] })
            this.mynahUI.updateStore(affectedTabId, { promptInputOptions: [], promptTopBarTitle: '' })
        }

        if (affectedTabId && this.isHybridChatEnabled) {
   1 change: 1 addition & 0 deletions1  
packages/core/src/shared/index.ts
Viewed
Original file line number	Diff line number	Diff line change
@@ -51,6 +51,7 @@ export * from './vscode/commands2'
export * from './utilities/pathUtils'
export * from './utilities/zipStream'
export * from './errors'
export { isTextEditor } from './utilities/editorUtilities'
export * as messages from './utilities/messages'
export * as errors from './errors'
export * as funcUtil from './utilities/functionUtils'
Footer
© 2025 GitHub, Inc.
Footer navigation
Terms
Privacy
Security
Status
Docs
Contact
Manage cookies
Do not share my personal information
"""


