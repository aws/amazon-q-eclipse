// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.util.EditorUtils;

public abstract class QGenericCommandHandler extends AbstractHandler {
	
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

}
