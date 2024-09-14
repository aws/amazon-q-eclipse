// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResult (
    /* 
     * See referenced type https://github.com/aws/aws-toolkit-visual-studio-staging/blob/9ed86affe1027a7fd62fa40bf05d2e130aec47cc/vspackages/AmazonQ/AwsToolkit.AmazonQ.Shared/Chat/Lsp/Protocols/ChatServerProtocols.cs#L32-L51
     */
    @JsonProperty("body") String body,
    @JsonProperty("messageId") String messageId,
    @JsonProperty("canBeVoted") Boolean canBeVoted,
    @JsonProperty("relatedContent") RelatedContent relatedContent,
    @JsonProperty("followUp") FollowUp followUp,
    @JsonProperty("codeReference") ReferenceTrackerInformation[] codeReference
) {};
