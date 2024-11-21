// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatUIInboundCommandTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String command = "command";
    private final String tabId = "tabId";
    private final Object params = new Object();
    private final Boolean isPartialResult = true;

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

    private final String selection = "selection";
    private final String triggerType = "triggerType";

    @Test
    void testRecordConstructionAndGetters() {
        ChatUIInboundCommand chatUiInboundCommand = new ChatUIInboundCommand(command, tabId, params, isPartialResult);

        assertEquals(command, chatUiInboundCommand.command());
        assertEquals(tabId, chatUiInboundCommand.tabId());
        assertEquals(params, chatUiInboundCommand.params());
        assertEquals(isPartialResult, chatUiInboundCommand.isPartialResult());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ChatResult chatResult = new ChatResult(body, messageId, canBeVoted, relatedContent, followUp,
                referenceTrackerInformationList);
        ChatUIInboundCommand chatUiInboundCommand = new ChatUIInboundCommand(command, tabId, chatResult,
                isPartialResult);

        String serializedObject = objectMapper.writeValueAsString(chatUiInboundCommand);

        assertEquals(
                "{\"command\":\"command\",\"tabId\":\"tabId\",\"params\":{\"body\":\"body\",\"messageId\":\"messageId\","
                        + "\"canBeVoted\":true,\"relatedContent\":{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":"
                        + "\"url\",\"body\":\"body\"}]},\"followUp\":{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\""
                        + "prompt\":\"Test prompt\",\"disabled\":false,\"description\":\"Test description\",\"type\":\"button\"}]},"
                        + "\"codeReference\":[{\"licenseName\":\"licenseName\",\"repository\":\"repository\",\"url\":\"url\","
                        + "\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":\"information\"}]},\"isPartialResult\":true}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"command\":\"command\",\"tabId\":\"tabId\",\"params\":{\"body\":\"body\",\"messageId\":\"messageId\","
                + "\"canBeVoted\":true,\"relatedContent\":{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":"
                + "\"url\",\"body\":\"body\"}]},\"followUp\":{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\""
                + "prompt\":\"Test prompt\",\"disabled\":false,\"description\":\"Test description\",\"type\":\"button\"}]},"
                + "\"codeReference\":[{\"licenseName\":\"licenseName\",\"repository\":\"repository\",\"url\":\"url\","
                + "\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":\"information\"}]},\"isPartialResult\":true}";

        ChatUIInboundCommand deserializedResult = objectMapper.readValue(json, ChatUIInboundCommand.class);

        assertNotNull(deserializedResult.params());
        assertEquals(command, deserializedResult.command());
        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(isPartialResult, deserializedResult.isPartialResult());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatUIInboundCommand.class));
    }

    @Test
    void testCreateGenericCommand() {
        GenericCommandParams genericParams = new GenericCommandParams(tabId, selection, triggerType,
                ChatUIInboundCommandName.GenericCommand.getValue());

        ChatUIInboundCommand chatUiInboundCommand = ChatUIInboundCommand.createGenericCommand(genericParams);

        assertNotNull(chatUiInboundCommand);
        assertEquals(ChatUIInboundCommandName.GenericCommand.getValue(), chatUiInboundCommand.command());
        assertNull(chatUiInboundCommand.tabId());
        assertEquals(genericParams, chatUiInboundCommand.params());
        assertNull(chatUiInboundCommand.isPartialResult());
    }

    @Test
    void testCreateSendToPromptCommand() {
        SendToPromptParams promptParams = new SendToPromptParams(selection, triggerType);

        ChatUIInboundCommand chatUiInboundCommand = ChatUIInboundCommand.createSendToPromptCommand(promptParams);

        assertNotNull(chatUiInboundCommand);
        assertEquals(ChatUIInboundCommandName.SendToPrompt.getValue(), chatUiInboundCommand.command());
        assertNull(chatUiInboundCommand.tabId());
        assertEquals(promptParams, chatUiInboundCommand.params());
        assertNull(chatUiInboundCommand.isPartialResult());
    }

}
