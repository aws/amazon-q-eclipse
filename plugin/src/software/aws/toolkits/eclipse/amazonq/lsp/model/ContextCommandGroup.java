package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

public record ContextCommandGroup(List<ContextCommand> commands) { }
