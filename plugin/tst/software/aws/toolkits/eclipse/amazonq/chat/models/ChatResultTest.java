// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatResultTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String body = "body";
    private final String messageId = "messageId";
    private final boolean canBeVoted = true;

    private final SourceLink sourceLink = new SourceLink("title", "url", "body");
    private final SourceLink[] sourceLinkArray = new SourceLink[] {sourceLink};
    private final RelatedContent relatedContent = new RelatedContent("title", sourceLinkArray);

    private final String pillText = "Click me";
    private final String prompt = "Test prompt";
    private final Boolean disabled = false;
    private final String description = "Test description";
    private final String type = "button";
    private final ChatItemAction chatItemAction = new ChatItemAction(pillText, prompt, disabled, description, type);

    private final String text = "text";
    private final ChatItemAction[] options = new ChatItemAction[] {chatItemAction};

    private final FollowUp followUp = new FollowUp(text, options);

    private final String licenseName = "licenseName";
    private final String repository = "repository";
    private final String url = "url";

    private final Integer startLine = 1;
    private final Integer endLine = 2;
    private final RecommendationContentSpan recommendationSpan = new RecommendationContentSpan(startLine, endLine);

    private final String information = "information";
    private final ReferenceTrackerInformation referenceTrackerInformation = new ReferenceTrackerInformation(licenseName,
            repository, url, recommendationSpan, information);

    private final ReferenceTrackerInformation[] referenceTrackerInformationList = new ReferenceTrackerInformation[] {
            referenceTrackerInformation};

    @Test
    void testRecordConstructionAndGetters() {
        ChatResult chatResult = new ChatResult(body, messageId, canBeVoted, relatedContent, followUp,
                referenceTrackerInformationList);

        assertEquals(body, chatResult.body());
        assertEquals(messageId, chatResult.messageId());
        assertEquals(canBeVoted, chatResult.canBeVoted());
        assertEquals(relatedContent, chatResult.relatedContent());
        assertEquals(followUp, chatResult.followUp());
        assertEquals(referenceTrackerInformationList, chatResult.codeReference());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ChatResult chatResult = new ChatResult(body, messageId, canBeVoted, relatedContent, followUp,
                referenceTrackerInformationList);

        String serializedObject = objectMapper.writeValueAsString(chatResult);

        assertEquals(serializedObject, "{\"body\":\"body\",\"messageId\":\"messageId\",\"canBeVoted\":true,"
                + "\"relatedContent\":{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}]}"
                + ",\"followUp\":{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,"
                + "\"description\":\"Test description\",\"type\":\"button\"}]},\"codeReference\":[{\"licenseName\":\"licenseName\",\"repository\""
                + ":\"repository\",\"url\":\"url\",\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":\"information\"}]}");
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"body\":\"body\",\"messageId\":\"messageId\",\"canBeVoted\":true,"
                + "\"relatedContent\":{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}]}"
                + ",\"followUp\":{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,"
                + "\"description\":\"Test description\",\"type\":\"button\"}]},\"codeReference\":[{\"licenseName\":\"licenseName\",\"repository\""
                + ":\"repository\",\"url\":\"url\",\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":\"information\"}]}";

        ChatResult deserializedResult = objectMapper.readValue(json, ChatResult.class);

        assertEquals(body, deserializedResult.body());
        assertEquals(messageId, deserializedResult.messageId());
        assertEquals(canBeVoted, deserializedResult.canBeVoted());
        assertEquals(relatedContent.title(), deserializedResult.relatedContent().title());

        assertEquals(1, deserializedResult.relatedContent().content().length);
        assertNotNull(deserializedResult.relatedContent().content()[0]);

        assertEquals(1, deserializedResult.followUp().options().length);
        assertNotNull(deserializedResult.followUp().options()[0]);

        assertEquals(1, deserializedResult.codeReference().length);
        assertNotNull(deserializedResult.codeReference()[0]);
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatResult.class));
    }

}
