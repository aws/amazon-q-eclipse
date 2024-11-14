package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;

public record InlineSuggestionCodeReference(
        @JsonProperty("references") InlineCompletionReference[] references,
        @JsonProperty("suggestionText") String suggestionText,
        @JsonProperty("filename") String filename,
        @JsonProperty("startLine") int startLine
    ) { }
