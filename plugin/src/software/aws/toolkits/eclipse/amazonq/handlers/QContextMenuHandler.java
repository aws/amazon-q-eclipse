// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.util.EditorUtils;

public abstract class QContextMenuHandler extends AbstractHandler {
	
	protected void executeGenericCommand(String genericCommandVerb) {
		String selection = EditorUtils.getSelectedText();
    	
    	GenericCommandParams params = new GenericCommandParams(
    			null,
    			selection,
    			TriggerType.ContextMenu.getValue(),
    			genericCommandVerb
    	);
    	
        ChatUIInboundCommand command = ChatUIInboundCommand.createGenericCommand(params);
        
        ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
	}

	protected void executeSendToPromptCommand() {
		String selection = EditorUtils.getSelectedText();
    	
    	SendToPromptParams params = new SendToPromptParams(
    			selection,
    			TriggerType.ContextMenu.getValue()
    	);
    	
        ChatUIInboundCommand command = ChatUIInboundCommand.createSendToPrompCommand(params);
        
        ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
	}
}
