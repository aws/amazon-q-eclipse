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
import java.util.stream.Collectors;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class InlineChatDiffManager {

    private record TextDiff(int offset, int length, boolean isDeletion) {
    }

    // Diff generation and rendering variables
    private String previousPartialResponse = null;
    private final String originalCode;
    private final int offset;
    private int previousDisplayLength;
    private final List<TextDiff> currentDiffs;
    private String ANNOTATION_ADDED;
    private String ANNOTATION_DELETED;
    private final ITextEditor editor;

    // Batching variables
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> pendingUpdate;
    private static final int BATCH_DELAY_MS = 200;
    private volatile long lastUpdateTime;

    public InlineChatDiffManager(final InlineChatTask task, final boolean isDarkTheme) {
        this.originalCode = task.getOriginalCode();
        this.currentDiffs = new ArrayList<>();
        this.previousDisplayLength = task.getOriginalCode().length();
        this.offset = task.getOffset();
        this.editor = task.getEditor();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        setColorPalette(isDarkTheme);
    }

    private void setColorPalette(final boolean isDark) {
        this.ANNOTATION_ADDED = "diffAnnotation.added";
        this.ANNOTATION_DELETED = "diffAnnotation.deleted";
        if (isDark) {
            this.ANNOTATION_ADDED += ".dark";
            this.ANNOTATION_DELETED += ".dark";
        }
    }

    public CompletableFuture<Void> processDiff(final ChatResult chatResult, final boolean isPartialResult) throws Exception {
        if (isPartialResult) {
            // Only process if content has changed
            if (!chatResult.body().equals(previousPartialResponse)) {
                if (pendingUpdate != null) {
                    pendingUpdate.cancel(false);
                }

                // Calculate remaining time since last UI update
                long currentTime = System.currentTimeMillis();
                long timeSinceUpdate = currentTime - lastUpdateTime;

                if (timeSinceUpdate >= BATCH_DELAY_MS) {
                    Activator.getLogger().info("Immediate update: " + timeSinceUpdate + "ms since last update");
                    // Push update immediately if enough time has passed
                    updateUI(chatResult);
                    lastUpdateTime = currentTime;
                } else {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    // Calculate remaining batch delay and schedule update
                    long delayToUse = BATCH_DELAY_MS - timeSinceUpdate;
                    Activator.getLogger().info("Scheduled update: waiting " + delayToUse + "ms");
                    pendingUpdate = executor.schedule(() -> {
                        try {
                            Activator.getLogger().info("Executing scheduled update after " + (System.currentTimeMillis() - lastUpdateTime) + "ms delay");
                            updateUI(chatResult);
                            lastUpdateTime = System.currentTimeMillis();
                            future.complete(null);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }

                    }, delayToUse, TimeUnit.MILLISECONDS);

                    return future;
                }

            }
        } else {
            // Final result - always update UI state regardless of content
            updateUI(chatResult);
            lastUpdateTime = System.currentTimeMillis();
        }

        return CompletableFuture.completedFuture(null);
    }

    private void updateUI(final ChatResult chatResult) throws Exception {
        Display.getDefault().syncExec(() -> {
            try {
                var newCode = unescapeChatResult(chatResult.body());
                computeDiffAndRenderOnEditor(newCode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean computeDiffAndRenderOnEditor(final String newCode) throws Exception {

        // Annotation model provides highlighting for the diff additions/deletions
        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

        // Clear existing diff annotations prior to starting new diff
        clearDiffAnnotations(annotationModel);

        // Restore document to original state -- may not be necessary anymore???
        document.replace(offset, previousDisplayLength, originalCode);

        // Split original and new code into lines for diff comparison
        String[] originalLines = originalCode.lines().toArray(String[]::new);
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
                currentDiffs.add(new TextDiff(offset + currentPos, line.length(), true));
                currentPos += line.length() + 1;
            }

            // Handle added lines and mark position
            for (String line : newChangedLines) {
                resultText.append(line).append("\n");
                currentDiffs.add(new TextDiff(offset + currentPos, line.length(), false));
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
        clearAnnotationsInRange(annotationModel, offset, offset + originalCode.length());

        // Apply new diff text
        document.replace(offset, originalCode.length(), finalText);

        // Add all annotations after text modifications are complete
        for (TextDiff diff : currentDiffs) {
            Position position = new Position(diff.offset(), diff.length());
            String annotationType = diff.isDeletion() ? ANNOTATION_DELETED : ANNOTATION_ADDED;
            String annotationText = diff.isDeletion() ? "Deleted Code" : "Added Code";
            annotationModel.addAnnotation(new Annotation(annotationType, false, annotationText), position);
        }

        // Store rendered text length for proper clearing next iteration
        previousDisplayLength = finalText.length();
        previousPartialResponse = newCode;
        return true;
    }

    public CompletableFuture<Void> handleDecision(final boolean userAcceptedChanges) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Display.getDefault().syncExec(() -> {
            try {
                var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
                final IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());

                // Filter diffs based on user decision
                List<TextDiff> diffsToRemove = currentDiffs.stream().filter(diff -> diff.isDeletion == userAcceptedChanges)
                        .sorted((a, b) -> Integer.compare(b.offset, a.offset)) // Sort in reverse order
                        .collect(Collectors.toList());

                for (TextDiff diff : diffsToRemove) {
                    int lineNumber = document.getLineOfOffset(diff.offset);
                    int lineStart = document.getLineOffset(lineNumber);
                    int lineLength = document.getLineLength(lineNumber);

                    document.replace(lineStart, lineLength, "");
                }

                clearDiffAnnotations(annotationModel);
                future.complete(null); // Complete the future when done

            } catch (final Exception e) {
                String action = userAcceptedChanges ? "Accepting" : "Declining";
                Activator.getLogger().error(action + " inline chat results failed with: " + e.getMessage(), e);
                future.completeExceptionally(e); // Complete exceptionally if there's an error
            }
        });

        return future;
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

    void cleanupState() {
        cancelBatchingOperations();
        this.currentDiffs.clear();
    }

    void restoreState() {
        final IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        clearDiffAnnotations(annotationModel);
    }

    private void cancelBatchingOperations() {
        try {
            if (pendingUpdate != null) {
                pendingUpdate.cancel(true);
            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cancelling async operations: " + e.getMessage(), e);
        }
    }
}
