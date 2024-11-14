// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.util.Map;
import java.util.Objects;

import software.aws.toolkits.eclipse.amazonq.telemetry.api.Emittable;
import software.aws.toolkits.eclipse.amazonq.telemetry.implementations.UITelemetryEmitter;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.TelemetryEventRecord;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.UITelemetryEventRecord;

public final class TelemetryEmitterManager {

    private UITelemetryEmitter uiTelemetryEmitter;

    private Map<Class<?>, Object> emitterMap;

    public TelemetryEmitterManager() {
        initializeEmitters();
        initializeMap();
    }

    @SuppressWarnings("unchecked")
    public <T extends TelemetryEventRecord> Emittable<T> getEmitterFor(final Class<T> type) {
        Objects.requireNonNull(type, "Type cannot be null");

        Emittable<T> emitter = (Emittable<T>) emitterMap.get(type);
        if (emitter == null) {
            throw new IllegalArgumentException("No emitter found for type: " + type.getName());
        }
        return emitter;
    }


    private void initializeEmitters() {
        uiTelemetryEmitter = new UITelemetryEmitter();
    }

    private void initializeMap() {
        emitterMap = Map.of(UITelemetryEventRecord.class, uiTelemetryEmitter);
    }

}
