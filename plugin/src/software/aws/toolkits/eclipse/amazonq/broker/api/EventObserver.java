// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface EventObserver<T> {

    // Reference:
    // https://stackoverflow.com/questions/3437897/how-do-i-get-a-class-instance-of-generic-type-t
    @SuppressWarnings("unchecked")
    default Class<T> getEventType() {
        Class<?> currentClass = getClass();
        while (currentClass != null) {
            for (Type type : currentClass.getGenericInterfaces()) {
                if (type instanceof ParameterizedType paramType && (paramType.getRawType() == EventObserver.class)) {
                    Type typeArg = paramType.getActualTypeArguments()[0];
                    if (typeArg instanceof Class<?>) {
                        return (Class<T>) typeArg;
                    }
                    throw new IllegalStateException("Generic type parameter is not a Class");
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new IllegalStateException("Could not determine generic type");
    }

    void onEvent(T event);

}
