package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public class InlineChatDiffManager {

    private static InlineChatDiffManager instance;
    private String ANNOTATION_ADDED;
    private String ANNOTATION_DELETED;
    private final ThemeDetector themeDetector;
    private List<TextDiff> currentDiffs;

    // Batching variables
    private ScheduledExecutorService executor;
    private static final int BATCH_DELAY_MS = 100;
    private InlineChatTask task;

    private InlineChatDiffManager() {
        themeDetector = new ThemeDetector();
    }

    static InlineChatDiffManager getInstance() {
        if (instance == null) {
            instance = new InlineChatDiffManager();
        }
        return instance;
    }

    void initNewTask(final InlineChatTask task) {
        this.task = task;
        this.currentDiffs = new ArrayList<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        setColorPalette(themeDetector.isDarkTheme());
    }

    CompletableFuture<Void> processDiff(final ChatResult chatResult, final boolean isPartialResult) throws Exception {
        if (isPartialResult) {
            // Only process if content has changed
            if (!chatResult.body().equals(task.getPreviousPartialResponse())) {
                if (task.getPendingUpdate() != null) {
                    task.getPendingUpdate().cancel(false);
                }

                // Calculate remaining time since last UI update
                long currentTime = System.currentTimeMillis();
                long timeSinceUpdate = currentTime - task.getLastUpdateTime();

                if (timeSinceUpdate >= BATCH_DELAY_MS) {
                    Activator.getLogger().info("Immediate update: " + timeSinceUpdate + "ms since last update");
                    // Push update immediately if enough time has passed
                    updateUI(chatResult);
                    task.setLastUpdateTime(currentTime);
                } else {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    // Calculate remaining batch delay and schedule update
                    long delayToUse = BATCH_DELAY_MS - timeSinceUpdate;
                    Activator.getLogger().info("Scheduled update: waiting " + delayToUse + "ms");
                    ScheduledFuture<?> pendingUpdate = executor.schedule(() -> {
                        try {
                            Activator.getLogger().info("Executing scheduled update after " + (System.currentTimeMillis() - task.getLastUpdateTime()) + "ms delay");
                            updateUI(chatResult);
                            task.setLastUpdateTime(System.currentTimeMillis());
                            future.complete(null);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }

                    }, delayToUse, TimeUnit.MILLISECONDS);

                    task.setPendingUpdate(pendingUpdate);
                    return future;
                }

            }
        } else {
            // Final result - always update UI state regardless of content
            updateUI(chatResult);
            task.setLastUpdateTime(System.currentTimeMillis());
        }

        return CompletableFuture.completedFuture(null);
    }

    private void updateUI(final ChatResult chatResult) throws Exception {
        if (!task.isActive()) {
            return;
        }
        final Exception[] ex = new Exception[1];
        Display.getDefault().syncExec(() -> {
            try {
                var newCode = unescapeChatResult(chatResult.body());
                computeDiffAndRenderOnEditor(newCode);
            } catch (Exception e) {
                ex[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
    }

    private boolean computeDiffAndRenderOnEditor(final String newCode) throws Exception {

        // Annotation model provides highlighting for the diff additions/deletions
        IAnnotationModel annotationModel = task.getEditor().getDocumentProvider().getAnnotationModel(task.getEditor().getEditorInput());
        var document = task.getEditor().getDocumentProvider().getDocument(task.getEditor().getEditorInput());

        // Clear existing diff annotations prior to starting new diff
        clearDiffAnnotations(annotationModel);

        // Split original and new code into lines for diff comparison
        String[] originalLines = (task.hasActiveSelection()) ? task.getOriginalCode().lines().toArray(String[]::new) : new String[0];
        String[] newLines = newCode.lines().toArray(String[]::new);

        // Diff generation --> returns Patch object which contains deltas for each line
        Patch<String> patch = DiffUtils.diff(Arrays.asList(originalLines), Arrays.asList(newLines));

        StringBuilder resultText = new StringBuilder();
        currentDiffs.clear(); // Clear previous diffs
        int currentPos = 0;
        int currentLine = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            // Continuously copy unchanged lines until we hit a diff
            while (currentLine < delta.getSource().getPosition()) {
                resultText.append(originalLines[currentLine]).append("\n");
                currentPos += originalLines[currentLine].length() + 1;
                currentLine++;
            }

            List<String> originalChangedLines = delta.getSource().getLines();
            List<String> newChangedLines = delta.getTarget().getLines();

            // Handle deleted lines and mark position
            for (String line : originalChangedLines) {
                resultText.append(line).append("\n");
                currentDiffs.add(new TextDiff(task.getOffset() + currentPos, line.length(), true));
                currentPos += line.length() + 1;
            }

            // Handle added lines and mark position
            for (String line : newChangedLines) {
                resultText.append(line).append("\n");
                currentDiffs.add(new TextDiff(task.getOffset() + currentPos, line.length(), false));
                currentPos += line.length() + 1;
            }

            currentLine = delta.getSource().getPosition() + delta.getSource().size();
        }
        // Loop through remaining unchanged lines
        while (currentLine < originalLines.length) {
            resultText.append(originalLines[currentLine]).append("\n");
            currentPos += originalLines[currentLine].length() + 1;
            currentLine++;
        }

        final String finalText = resultText.toString();

        // Clear existing annotations in the affected range
        clearAnnotationsInRange(annotationModel, task.getOffset(), task.getOffset() + task.getOriginalCode().length());

        // Apply new diff text
        document.replace(task.getOffset(), task.getPreviousDisplayLength(), finalText);

        // Add all annotations after text modifications are complete
        for (TextDiff diff : currentDiffs) {
            Position position = new Position(diff.offset(), diff.length());
            String annotationType = diff.isDeletion() ? ANNOTATION_DELETED : ANNOTATION_ADDED;
            String annotationText = diff.isDeletion() ? "Deleted Code" : "Added Code";
            annotationModel.addAnnotation(new Annotation(annotationType, false, annotationText), position);
        }

        // Store rendered text length for proper clearing next iteration
        task.setPreviousDisplayLength(finalText.length());
        task.setPreviousPartialResponse(newCode);
        return true;
    }

    CompletableFuture<Void> handleDecision(final boolean userAcceptedChanges) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        var typeToRemove = (userAcceptedChanges) ? ANNOTATION_DELETED : ANNOTATION_ADDED;

        Display.getDefault().syncExec(() -> {
            try {
                var document = task.getEditor().getDocumentProvider().getDocument(task.getEditor().getEditorInput());
                final IAnnotationModel annotationModel = task.getEditor().getDocumentProvider().getAnnotationModel(task.getEditor().getEditorInput());

                // Collect lines to remove
                List<Position> linesToRemove = new ArrayList<>();
                Iterator<?> annotations = annotationModel.getAnnotationIterator();

                // Iterate over annotations to guarantee editor changes don't cause incorrect
                // code placement and deletions
                while (annotations.hasNext()) {
                    var obj = annotations.next();
                    if (obj instanceof Annotation) {
                        Annotation annotation = (Annotation) obj;
                        Position position = annotationModel.getPosition(annotation);
                        if (position != null && typeToRemove.equals(annotation.getType())) {
                            linesToRemove.add(position);
                        }
                    }
                }

                // Sort in reverse order to maintain valid offsets when removing
                linesToRemove.sort((a, b) -> Integer.compare(b.offset, a.offset));

                // Remove the lines
                for (Position pos : linesToRemove) {
                    int lineNumber = document.getLineOfOffset(pos.offset);
                    int lineStart = document.getLineOffset(lineNumber);
                    int lineLength = document.getLineLength(lineNumber);
                    document.replace(lineStart, lineLength, "");
                }

                clearDiffAnnotations(annotationModel);
                future.complete(null);

            } catch (final Exception e) {
                String action = userAcceptedChanges ? "Accepting" : "Declining";
                Activator.getLogger().error(action + " inline chat results failed with: " + e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    void cleanupState() {
        restoreState();
        cancelBatchingOperations();
        currentDiffs.clear();
    }

    void restoreState() {
        try {
            final IAnnotationModel annotationModel = task.getEditor().getDocumentProvider().getAnnotationModel(task.getEditor().getEditorInput());
            clearDiffAnnotations(annotationModel);
        } catch (Exception e) {
            Activator.getLogger().error("Failed to restore state in diff manager: " + e.getMessage(), e);
        }

    }

    private void setColorPalette(final boolean isDark) {
        this.ANNOTATION_ADDED = "diffAnnotation.added";
        this.ANNOTATION_DELETED = "diffAnnotation.deleted";
        if (isDark) {
            this.ANNOTATION_ADDED += ".dark";
            this.ANNOTATION_DELETED += ".dark";
        }
    }

    private void clearDiffAnnotations(final IAnnotationModel annotationModel) {
        var annotations = annotationModel.getAnnotationIterator();
        while (annotations.hasNext()) {
            var annotation = annotations.next();
            String type = annotation.getType();
            if (type.startsWith("diffAnnotation.")) {
                annotationModel.removeAnnotation(annotation);
            }
        }
    }

    private void clearAnnotationsInRange(final IAnnotationModel model, final int start, final int end) {
        Iterator<Annotation> iterator = model.getAnnotationIterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            Position position = model.getPosition(annotation);
            if (position != null && position.offset >= start && position.offset + position.length <= end) {
                model.removeAnnotation(annotation);
            }
        }
    }

    private String unescapeChatResult(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("=&lt;", "=<").replace("&lt;=", "<=").replace("&gt;", ">")
                .replace("=&gt;", "=>").replace("&gt;=", ">=").replace("&nbsp;", " ").replace("&lsquo;", "'").replace("&rsquo;", "'").replace("&amp;", "&");
    }

    private void cancelBatchingOperations() {
        try {
            if (task.getPendingUpdate() != null) {
                task.getPendingUpdate().cancel(true);
            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cancelling async operations: " + e.getMessage(), e);
        }
    }
}
