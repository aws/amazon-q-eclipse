// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AmazonQChatWebview extends ViewPart implements ISelectionListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    @Inject
    private Shell shell;
    private Browser browser;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    private boolean darkMode = Display.isSystemDarkTheme();

    public AmazonQChatWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new AmazonQChatViewActionHandler();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        browser = new Browser(parent, SWT.NATIVE);
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);

        browser.setBackground(black);
        parent.setBackground(black);
        browser.setText(getContent());

        BrowserFunction prefsFunction = new OpenPreferenceFunction(browser, "openEclipsePreferences", this::openPreferences);
        browser.addDisposeListener(e -> prefsFunction.dispose());

        createActions();
        contributeToActionBars(getViewSite());
        getSite().getPage().addSelectionListener(this);

       new BrowserFunction(browser, "ideCommand") {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(parsedCommand -> actionHandler.handleCommand(parsedCommand, browser));
                return null;
            }
        };
        
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(changeThemeAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(changeThemeAction);
    }

    private void createActions() {
        changeThemeAction = new ChangeThemeAction();
    }

    private Action changeThemeAction;

    private class ChangeThemeAction extends Action {
        ChangeThemeAction() {
            setText("Change Color");
            setToolTipText("Change the color");
            setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
        }

        @Override
        public void run() {
            darkMode = !darkMode;
            browser.execute("changeTheme(" + darkMode + ");");
        }
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
                + "			});\n"
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
    public final void dispose() {
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

}
