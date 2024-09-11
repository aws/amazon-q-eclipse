// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

import jakarta.inject.Inject;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    @Inject
    private Shell shell;
    private Browser browser;
    private AuthStatusChangedListener authStatusChangedListener;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public ToolkitLoginWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        browser = new Browser(parent, SWT.NATIVE);
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);

        browser.setBackground(black);
        parent.setBackground(black);

        AuthUtils.isLoggedIn().thenAcceptAsync(isLoggedIn -> {
            handleAuthStatusChange(isLoggedIn);
        }, ThreadingUtils::executeAsyncTask);
        createActions(true);

        BrowserFunction prefsFunction = new OpenPreferenceFunction(browser, "openEclipsePreferences", this::openPreferences);
        browser.addDisposeListener(e -> prefsFunction.dispose());

        contributeToActionBars(getViewSite());
        getSite().getPage().addSelectionListener(this);
        AuthUtils.addAuthStatusChangeListener(this::updateSignoutActionVisibility);
        authStatusChangedListener = this::handleAuthStatusChange;

        new BrowserFunction(browser, ViewConstants.COMMAND_FUNCTION_NAME) {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(command -> actionHandler.handleCommand(command, browser));
                return null;
            }
        };
    }


    private void handleAuthStatusChange(final boolean isLoggedIn) {
        Display.getDefault().asyncExec(() -> {
            updateSignoutActionVisibility(isLoggedIn);
            if (!isLoggedIn) {
                browser.setText(getContent());
            } else {
                browser.setText("Signed in");
            }
        });
    }


    @Override
    public final void setFocus() {
        browser.setFocus();
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
    public final void dispose() {
        AuthUtils.removeAuthStatusChangeListener(authStatusChangedListener);
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }
}
