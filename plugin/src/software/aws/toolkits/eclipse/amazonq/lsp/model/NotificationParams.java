package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;
import java.util.Optional;

import org.eclipse.lsp4j.MessageType;

public record NotificationParams(
    MessageType type,
    NotificationContent content,
    Optional<EventIdentifier> id,
    Optional<List<NotificationAction>> actions
) { }