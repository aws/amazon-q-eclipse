// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;
import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.CodeReferenceLogItem;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

public final class DefaultCodeReferenceLoggingService implements CodeReferenceLoggingService {
    private static final String SEPARATOR = "----------------------------------------\n";

    private static DefaultCodeReferenceLoggingService instance;

    private DefaultCodeReferenceLoggingService() {
        // Prevent instantiation
    }

    public static synchronized DefaultCodeReferenceLoggingService getInstance() {
        if (instance == null) {
            instance = new DefaultCodeReferenceLoggingService();
        }
        return instance;
    }

    @Override
    public void log(final InlineSuggestionCodeReference codeReference) {
        if (codeReference.references() == null || codeReference.references().length == 0) {
            return;
        }

        for (var reference : codeReference.references()) {
            String filename = codeReference.filename();
            String suggestionText = codeReference.suggestionText();
            int suggestionTextDepth = suggestionText.split("\n").length;
            int startLine = codeReference.startLine();
            int endLine = startLine + suggestionTextDepth;
            String licenseName = reference.getLicenseName();
            String referenceName = reference.getReferenceName();
            String referenceUrl = reference.getReferenceUrl();

            String message = createInlineSuggestionLogMessage(filename, startLine, endLine, licenseName, referenceName, referenceUrl, suggestionText);
            CodeReferenceLogItem logItem = new CodeReferenceLogItem(message);
            CodeReferenceLoggedProvider.notifyCodeReferenceLogged(logItem);
        }
    }

    private String createInlineSuggestionLogMessage(final String filename, final int startLine, final int endLine, final String licenseName,
            final String referenceName, final String referenceUrl, final String suggestionText) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR);
        sb.append(String.format("%s(%d,%d): Accepted recommendation with license: (%s)\n", filename, startLine, endLine, licenseName));
        sb.append(String.format("From %s (%s):\n", referenceName, referenceUrl));
        sb.append(SEPARATOR);
        sb.append(String.format("%s\n", suggestionText));
        sb.append(SEPARATOR);
        return sb.toString();
    }

    @Override
    public void log(final ChatCodeReference codeReference) {
        if (codeReference.references() == null || codeReference.references().length == 0) {
            return;
        }

        for (ReferenceTrackerInformation reference : codeReference.references()) {
            String licenseName = reference.licenseName();
            String repository = reference.repository();
            String repositoryUrl = reference.url();

            String message = createChatLogMessage(licenseName, repository, repositoryUrl);
            CodeReferenceLogItem logItem = new CodeReferenceLogItem(message);
            CodeReferenceLoggedProvider.notifyCodeReferenceLogged(logItem);
        }
    }

    private String createChatLogMessage(final String licenseName, final String repository, final String repositoryUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR);
        sb.append(String.format("Reference with license: %s\n", licenseName));
        sb.append(String.format("From %s (%s)\n", repository, repositoryUrl));
        sb.append(SEPARATOR);
        return sb.toString();
    }
}
