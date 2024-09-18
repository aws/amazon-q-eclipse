// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    @Inject
    private Shell shell;
    private Browser browser;
    private AmazonQCommonActions amazonQCommonActions;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public AmazonQChatWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new AmazonQChatViewActionHandler();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        setupAmazonQView(parent, true);
        browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();

        AuthUtils.isLoggedIn().thenAcceptAsync(isLoggedIn -> {
            handleAuthStatusChange(isLoggedIn);
        }, ThreadingUtils::executeAsyncTask);

        BrowserFunction prefsFunction = new OpenPreferenceFunction(browser, "openEclipsePreferences", this::openPreferences);
        browser.addDisposeListener(e -> prefsFunction.dispose());

       new BrowserFunction(browser, "ideCommand") {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(parsedCommand -> actionHandler.handleCommand(parsedCommand, browser));
                return null;
            }
        };
    }

    private class OpenPreferenceFunction extends BrowserFunction {
        private Runnable function;

        OpenPreferenceFunction(final Browser browser, final String name, final Runnable function) {
            super(browser, name);
            this.function = function;
        }

        @Override
        public Object function(final Object[] arguments) {
            function.run();
            return getName() + " executed!";
        }
    }

    private void openPreferences() {
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, null, null, null);
        dialog.open();
    }

    private String getContent() {
        String jsFile = PluginUtils.getAwsDirectory(LspConstants.LSP_SUBDIRECTORY).resolve("amazonq-ui.js").toString();
        return String.format("<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>Chat UI</title>\n"
                + "    %s\n"
                + "</head>\n"
                + "<body>\n"
                + "    %s\n"
                + "</body>\n"
                + "</html>", generateCss(), generateJS(jsFile));
    }

    private String generateCss() {
        return "<style>\n"
                + "        body,\n"
                + "        html {\n"
                + "            background-color: var(--mynah-color-bg);\n"
                + "            color: var(--mynah-color-text-default);\n"
                + "            height: 100vh;\n"
                + "            width: 100%%;\n"
                + "            overflow: hidden;\n"
                + "            margin: 0;\n"
                + "            padding: 0;\n"
                + "        }\n"
                + "        textarea:placeholder-shown {\n"
                + "            line-height: 1.5rem;\n"
                + "        }"
                + "    </style>";
    }

    private String generateJS(final String jsEntrypoint) {
        return String.format("<script type=\"text/javascript\" src=\"%s\" defer onload=\"init()\"></script>\n"
                + "    <script type=\"text/javascript\">\n"
                + "        const init = () => {\n"
                + "            amazonQChat.createChat({\n"
                + "               postMessage: (message) => {\n"
                + "                    ideCommand(JSON.stringify(message));\n"
                + "               }\n"
                + "         });\n"
                + "        }\n"
                + "    </script>", jsEntrypoint);
    }

    @Override
    public final void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (selection.isEmpty()) {
            return;
        }
        if (selection instanceof IStructuredSelection) {
            browser.execute("setSelection(\"" + part.getTitle() + "::"
                    + ((IStructuredSelection) selection).getFirstElement().getClass().getSimpleName() + "\");");
        } else {
            browser.execute("setSelection(\"Something was selected in part " + part.getTitle() + "\");");
        }
    }

    @Override
    protected final void handleAuthStatusChange(final boolean isLoggedIn) {
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(isLoggedIn, getViewSite());
            if (!isLoggedIn) {
                browser.setText("Signed Out");
                AmazonQView.showView(ToolkitLoginWebview.ID);
            } else {
                browser.setText(getContent());
            }
        });
    }

}
