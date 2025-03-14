// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.ISources;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

public final class MenuContributionFactory extends AbstractContributionFactory {

    private List<IContributionItem> items = new ArrayList<>();

    public MenuContributionFactory(final String location) {
        super("menu:" + location, null);
    }

    public void addContributionItemsFromMenu(final IMenuManager menuManager) {
        if (!items.isEmpty()) {
            return;
        }

        for (IContributionItem item : menuManager.getItems()) {
            items.add(item);
        }
    }

    @Override
    public void createContributionItems(final IServiceLocator serviceLocator, final IContributionRoot additions) {
        for (IContributionItem item : items) {
            additions.addContributionItem(item, new Expression() {
                @Override
                public void collectExpressionInfo(final ExpressionInfo info) {
                    info.markDefaultVariableAccessed();
                    info.addVariableNameAccess(ISources.ACTIVE_MENU_NAME);
                }

                @Override
                public EvaluationResult evaluate(final IEvaluationContext context) {
                    return EvaluationResult.valueOf(Boolean.TRUE.equals(item.isVisible()));
                }
            });
        }
    }

}
