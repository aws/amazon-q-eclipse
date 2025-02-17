// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;

public abstract class BaseAmazonQView {

    private IViewSite viewSite;

    private AmazonQCommonActions amazonQCommonActions;
    private AmazonQStaticActions amazonQStaticActions;

    private Disposable signOutActionAuthStateSubscription;
    private Disposable feedbackDialogAuthStateSubscription;
    private Disposable customizationDialogAuthStateSubscription;
    private Disposable toggleAutoTriggerAuthStateSubscription;

    public abstract Composite setupView(Composite parentComposite);

    public final void setViewSite(final IViewSite viewSite) {
        this.viewSite = viewSite;
    }

    protected final void setupAmazonQCommonActions() {
        if (viewSite == null) {
            Activator.getLogger().info("View Site is null for creating AmazonQCommonActions");
            return;
        }

        amazonQCommonActions = new AmazonQCommonActions(viewSite);
        setupCommonActionAuthListeners();
        viewSite.getActionBars().updateActionBars();
    }

    protected final void setupAmazonQStaticActions() {
        if (viewSite == null) {
            Activator.getLogger().info("View Site is null for creating AmazonQStaticActions");
            return;
        }

        amazonQStaticActions = new AmazonQStaticActions(viewSite);
        viewSite.getActionBars().updateActionBars();

    }

    protected final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
    }

    protected final AmazonQStaticActions getAmazonQStaticActions() {
        return amazonQStaticActions;
    }

    protected final Image loadImage(final String imagePath) {
        Image loadedImage = null;
        try {
            URL imageUrl = PluginUtils.getResource(imagePath);
            if (imageUrl != null) {
                loadedImage = new Image(Display.getCurrent(), imageUrl.openStream());
            }
        } catch (IOException e) {
            Activator.getLogger().warn(e.getMessage(), e);
        }
        return loadedImage;
    }

    protected final Font magnifyFontSize(final Composite parentComposite, final Font originalFont, final int fontSize) {
        FontData[] fontData = originalFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(fontSize);
        }
        Font magnifiedFont = new Font(parentComposite.getDisplay(), fontData);
        return magnifiedFont;
    }

    private void setupCommonActionAuthListeners() {
        signOutActionAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getSignoutAction());
        feedbackDialogAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getFeedbackDialogContributionAction());
        customizationDialogAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getCustomizationDialogContributionAction());
        toggleAutoTriggerAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getToggleAutoTriggerContributionAction());
    }

    /**
     * Disposes of resources and cleans up subscriptions when the view is closed.
     * This method ensures proper cleanup of authentication state subscriptions to prevent memory leaks.
     *
     * The following subscriptions are disposed:
     * - Sign out action authentication state
     * - Feedback dialog authentication state
     * - Customization dialog authentication state
     * - Auto-trigger toggle authentication state
     *
     * Each subscription is checked for null and disposal state before being disposed
     * to prevent potential null pointer exceptions.
     */
    public void dispose() {
        if (amazonQCommonActions != null) {
            amazonQCommonActions.dispose();
            amazonQCommonActions = null;
        }

        if (amazonQStaticActions != null) {
            amazonQStaticActions.dispose();
            amazonQStaticActions = null;
        }

        if (signOutActionAuthStateSubscription != null && !signOutActionAuthStateSubscription.isDisposed()) {
            signOutActionAuthStateSubscription.dispose();
            signOutActionAuthStateSubscription = null;
        }
        if (feedbackDialogAuthStateSubscription != null && !feedbackDialogAuthStateSubscription.isDisposed()) {
            feedbackDialogAuthStateSubscription.dispose();
            feedbackDialogAuthStateSubscription = null;
        }
        if (customizationDialogAuthStateSubscription != null
                && !customizationDialogAuthStateSubscription.isDisposed()) {
            customizationDialogAuthStateSubscription.dispose();
            customizationDialogAuthStateSubscription = null;
        }
        if (toggleAutoTriggerAuthStateSubscription != null && !toggleAutoTriggerAuthStateSubscription.isDisposed()) {
            toggleAutoTriggerAuthStateSubscription.dispose();
            toggleAutoTriggerAuthStateSubscription = null;
        }
    }

}
