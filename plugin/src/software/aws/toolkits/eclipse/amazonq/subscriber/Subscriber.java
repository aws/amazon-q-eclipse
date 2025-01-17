// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.subscriber;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface Subscriber<T> {

    @SuppressWarnings("unchecked")
    default Class<T> getEventType() {
        Class<?> currentClass = getClass();
        while (currentClass != null) {
            Type[] interfaces = currentClass.getGenericInterfaces();
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) type;
                    if (paramType.getRawType() == Subscriber.class) {
                        return (Class<T>) paramType.getActualTypeArguments()[0];
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new IllegalStateException("Could not determine generic type");
    }

    void handleEvent(T event);
    void handleError(Throwable error);

}
