package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptInputOptionChangeParams(
        @JsonProperty("tabId") String tabId,
        @JsonProperty("optionsValues") Map<String, String> optionValues,
        @JsonProperty("eventId") String eventId) {
}
