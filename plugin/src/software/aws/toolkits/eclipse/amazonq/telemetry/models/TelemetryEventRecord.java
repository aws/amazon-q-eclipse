package software.aws.toolkits.eclipse.amazonq.telemetry.models;

public class TelemetryEventRecord {

    public TelemetryEventRecord() {
        super();
    }

    protected abstract static class TelemetryEventBuilder<T extends TelemetryEventRecord, B extends TelemetryEventBuilder<T, B>> {

        protected abstract B self();
        public abstract T build();

    }

}
