package software.aws.toolkits.eclipse.amazonq.inlineChat;

public record InlineChatResultParams(
        int inputLength,
        int numSelectedLines,
        int numSuggestionAddChars,
        int numSuggestionAddLines,
        int numSuggestionDelChars,
        int numSuggestionDelLines,
        UserDecision userDecision,
        double startLatency,
        double endLatency) {

}
