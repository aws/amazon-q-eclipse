package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.time.Instant;

import software.aws.toolkits.eclipse.amazonq.inlineChat.InlineChatResultParams;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.CodewhispererTelemetry;

public final class CodeWhispererTelemetryProvider {

    private CodeWhispererTelemetryProvider() {
        //prevent instantiation
    }

    public static void emitInlineChatEventMetric(final InlineChatResultParams params) {
        var metadata = CodewhispererTelemetry.InlineChatEventEvent()
                .requestId(params.requestId())
                .programmingLanguage(params.language())
                .inputLength(params.inputLength())
                .numSelectedLines(params.numSelectedLines())
                .numSuggestionAddChars(params.numSuggestionAddChars())
                .numSuggestionAddLines(params.numSuggestionAddLines())
                .numSuggestionDelChars(params.numSuggestionDelChars())
                .numSuggestionDelLines(params.numSuggestionDelLines())
                .userDecision(params.userDecision())
                .responseStartLatency(params.startLatency())
                .responseEndLatency(params.endLatency())
                .codeIntent(true)
                .passive(false)
                .createTime(Instant.now())
                .value(1.0)
                .build();
        Activator.getTelemetryService().emitMetric(metadata);
    }

}
