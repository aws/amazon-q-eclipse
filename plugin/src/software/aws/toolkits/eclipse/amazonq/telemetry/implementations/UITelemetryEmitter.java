package software.aws.toolkits.eclipse.amazonq.telemetry.implementations;

import java.time.Instant;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.aws.toolkits.eclipse.amazonq.telemetry.api.Emittable;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.EmittableEventType;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.UITelemetryEventRecord;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;
import software.aws.toolkits.telemetry.UiTelemetry;

public class UITelemetryEmitter extends Emittable<UITelemetryEventRecord> {

    @Override
    protected final MetricDatum getMetricDatum(final EmittableEventType type, final UITelemetryEventRecord event) {
        if (type != EmittableEventType.UI_ELEMENT_CLICK_EVENT) {
            throw new IllegalArgumentException("Invalid event type for UI telemetry");
        }

        return UiTelemetry.ClickEvent()
                .elementId(event.elementId())
                .passive(false)
                .createTime(Instant.now())
                .result(Result.SUCCEEDED)
                .build();
    }

}
