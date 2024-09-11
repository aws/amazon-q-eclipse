// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public abstract class AmazonQView extends ViewPart implements ISelectionListener {
	
	private Browser browser;
	private boolean darkMode = Display.isSystemDarkTheme();
	
    private Action changeThemeAction;
    private Action signoutAction;
    

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

    private class SignoutAction extends Action {
        SignoutAction() {
            setText("Sign out");
        }

        @Override
        public void run() {
            AuthUtils.invalidateToken();
            showView(ToolkitLoginWebview.ID);
        }
    }
    
    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(changeThemeAction);
        manager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(changeThemeAction);
    }
    
    protected void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }
    
    protected void updateSignoutActionVisibility(final boolean isLoggedIn) {
        signoutAction.setEnabled(isLoggedIn);
    }
    
    protected void createActions(final boolean isLoggedIn) {
        changeThemeAction = new ChangeThemeAction();
        signoutAction = new SignoutAction();
        updateSignoutActionVisibility(isLoggedIn);
    }
    
    protected void showView(String viewId) {	
    	IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    	if (page != null) {
    		try {
    			page.showView(viewId);
    		} catch (Exception e) {
    			PluginLogger.error("Error occurred while showing view (" + viewId + ")", e);
    		}
    	}
    }

}