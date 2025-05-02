// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ChatAsyncResultManager {
    private static ChatAsyncResultManager instance;
    private Map<String, CompletableFuture<Object>> results;
    private final long defaultTimeout;
    private final TimeUnit defaultTimeUnit;

    public ChatAsyncResultManager(final long timeout, final TimeUnit timeUnit) {
        results = new ConcurrentHashMap<>();
        this.defaultTimeout = timeout;
        this.defaultTimeUnit = timeUnit;
    }

    public static synchronized ChatAsyncResultManager getInstance() {
        if (instance == null) {
            instance = new ChatAsyncResultManager(30, TimeUnit.SECONDS);
        }
        return instance;
    }

    public void createRequestId(final String requestId) {
        results.put(requestId, new CompletableFuture<>());
    }

    public void removeRequestId(String requestId) {
        CompletableFuture<Object> future = results.remove(requestId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    public void setResult(final String requestId, final Object result) {
        CompletableFuture<Object> future = results.get(requestId);
        if (future != null) {
            future.complete(result);
        }
    }

    public Object getResult(final String requestId) throws Exception {
        return getResult(requestId, defaultTimeout, defaultTimeUnit);
    }

    public Object getResult(final String requestId, final long timeout, final TimeUnit unit) throws Exception {
        CompletableFuture<Object> future = results.get(requestId);
        if (future == null) {
            throw new IllegalArgumentException("Request ID not found: " + requestId);
        }

        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            results.remove(requestId);
            throw new TimeoutException("Operation timed out for requestId: " + requestId);
        }
    }
}
