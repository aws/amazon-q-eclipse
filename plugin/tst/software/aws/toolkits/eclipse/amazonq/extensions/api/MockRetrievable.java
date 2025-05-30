// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.api;

import org.mockito.MockedStatic;

public interface MockRetrievable<T> {

    <U> U getMock(Class<U> type);
    MockedStatic<T> getStaticMock();

}
