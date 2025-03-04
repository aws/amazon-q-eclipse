// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

public class MenuContributionFactory extends AbstractContributionFactory {

    private List<IContributionItem> items = new ArrayList<>();

    public MenuContributionFactory(String location) {
        super("menu:" + location, null);
    }

    public void addContributionItem(IContributionItem item) {
        items.add(item);
    }

    public void addAction(IAction action) {
        items.add(new ActionContributionItem(action));
    }

    @Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
        for (IContributionItem item : items) {
            additions.addContributionItem(item, null);
        }
    }

}
