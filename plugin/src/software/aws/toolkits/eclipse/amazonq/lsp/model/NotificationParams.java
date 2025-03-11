package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

import org.eclipse.lsp4j.MessageType;

public record NotificationParams(
    MessageType type,
    NotificationContent content,
    String id,
    List<NotificationAction> actions
) { }