// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

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
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    @Inject
    private Shell shell;
    private Browser browser;
    private AmazonQCommonActions amazonQCommonActions;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public ToolkitLoginWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
    }

    @Override
    public void createPartControl(final Composite parent) {
        setupAmazonQView(parent, true);
        browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();

        AuthUtils.isLoggedIn().thenAcceptAsync(isLoggedIn -> {
            handleAuthStatusChange(isLoggedIn);
        }, ThreadingUtils::executeAsyncTask);

        BrowserFunction prefsFunction = new OpenPreferenceFunction(browser, "openEclipsePreferences", this::openPreferences);
        browser.addDisposeListener(e -> prefsFunction.dispose());


        new BrowserFunction(browser, ViewConstants.COMMAND_FUNCTION_NAME) {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(command -> actionHandler.handleCommand(command, browser));
                return null;
            }
        };
    }

    protected void handleAuthStatusChange(final boolean isLoggedIn) {
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(isLoggedIn, getViewSite());
            if (!isLoggedIn) {
                browser.setText(getContent());
            } else {
                browser.setText("Signed in");
                AmazonQView.showView(AmazonQChatWebview.ID);
            }
        });
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
        try {
            URL jsFile = PluginUtils.getResource("webview/build/assets/js/getStart.js");
            return String.format("<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "    <head>\n"
                    + "        <title>AWS Q</title>\n"
                    + "    </head>\n"
                    + "    <body class=\"jb-light\">\n"
                    + "        <div id=\"app\"></div>\n"
                    + "        <script type=\"text/javascript\" src=\"%s\"></script>\n"
                    + "        <script>\n"
                    + "            window.addEventListener('DOMContentLoaded', function() {\n"
                    + "                const ideApi = {\n"
                    + "                    postMessage(message) {\n"
                    + "                        ideCommand(JSON.stringify(message));\n"
                    + "                    }\n"
                    + "                };\n"
                    + "                window.ideApi = ideApi;\n"
                    + "            });\n"
                    + "        </script>\n"
                    + "    </body>\n"
                    + "</html>", jsFile.toString());
        } catch (IOException e) {
            return "Failed to load JS";
        }
    }

    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
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
}
