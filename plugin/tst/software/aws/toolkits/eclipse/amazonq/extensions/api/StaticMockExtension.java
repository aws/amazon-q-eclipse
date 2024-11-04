// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.api;

import java.util.Map;
import java.util.Optional;

public abstract class StaticMockExtension<T> implements MockRetrievable<T> {
    private Map<Class<?>, Object> mocksMap;

    @Override
    public final <U> Optional<U> getMock(final Class<U> type) {
        return Optional.ofNullable(mocksMap.get(type))
                .filter(type::isInstance)
                .map(type::cast);
    }

    public final Map<Class<?>, Object> getMocksMap() {
        return mocksMap;
    }

    public final void setMocksMap(final Map<Class<?>, Object> mocksMap) {
        this.mocksMap = mocksMap;
    }

}
