// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.widgets.Composite;

public interface BaseAmazonQView {
    public Composite setupView(Composite parentComposite);
    public boolean canDisplay();
    public void dispose();
}
