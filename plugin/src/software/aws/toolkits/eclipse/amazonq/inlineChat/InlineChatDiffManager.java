// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatResult;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class InlineChatDiffManager {

    private static InlineChatDiffManager instance;
    private String annotationAdded;
    private String annotationDeleted;
    private List<TextDiff> currentDiffs;
    private InlineChatTask task;

    private InlineChatDiffManager() {
        // Prevent instantiation
    }

    static InlineChatDiffManager getInstance() {
        if (instance == null) {
            instance = new InlineChatDiffManager();
        }
        return instance;
    }

    void initNewTask(final InlineChatTask task, final boolean isDarkTheme) {
        this.task = task;
        this.currentDiffs = new ArrayList<>();
        setColorPalette(isDarkTheme);
    }
    synchronized CompletableFuture<Void> processDiff(final InlineChatResult chatResult, final boolean isPartialResult) throws Exception {
        if (!task.isActive()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> diffFuture;
        if (isPartialResult) {
            // Only process if content has changed
            if (!chatResult.body().equals(task.getPreviousPartialResponse())) {
                diffFuture = updateUI(chatResult);
                diffFuture.thenRun(() -> {
                    if (task.getFirstTokenTime() == -1) {
                        task.setFirstTokenTime(System.currentTimeMillis());
                    }
                });
            } else {
                diffFuture = CompletableFuture.completedFuture(null);
            }
        } else {
            // Final result - always update UI state regardless of content
            diffFuture = updateUI(chatResult);
            diffFuture.thenRun(() -> {
                task.setLastTokenTime(System.currentTimeMillis());
            });
            task.setTextDiffs(currentDiffs);
        }
        return diffFuture;
    }

    private CompletableFuture<Void> updateUI(final InlineChatResult chatResult) throws Exception {
        if (!task.isActive()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        Display.getDefault().syncExec(() -> {
            try {
                var newCode = unescapeChatResult(chatResult.body());
                computeDiffAndRenderOnEditor(newCode);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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
        int deletedLines = 0;
        int insertedLines = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {

            // Count deletion and addition lines for telemetry
            if (delta.getType() == DeltaType.DELETE || delta.getType() == DeltaType.CHANGE) {
                deletedLines += delta.getSource().getLines().size();
            }
            if (delta.getType() == DeltaType.INSERT || delta.getType() == DeltaType.CHANGE) {
                insertedLines += delta.getTarget().getLines().size();
            }

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
                currentDiffs.add(new TextDiff(task.getSelectionOffset() + currentPos, line.length(), true));
                currentPos += line.length() + 1;
            }

            // Handle added lines and mark position
            for (String line : newChangedLines) {
                resultText.append(line).append("\n");
                currentDiffs.add(new TextDiff(task.getSelectionOffset() + currentPos, line.length(), false));
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

        var originalEndsInNewLine = task.getOriginalCode().endsWith("\n");
        var diffEndsInNewLine = resultText.length() > 0 && resultText.charAt(resultText.length() - 1) == '\n';

        if (!originalEndsInNewLine && diffEndsInNewLine) {
            resultText.setLength(resultText.length() - 1);
        }
        final String finalText = resultText.toString();

        // Clear existing annotations in the affected range
        clearAnnotationsInRange(annotationModel, task.getSelectionOffset(), task.getSelectionOffset() + task.getOriginalCode().length());

        // Apply new diff text
        document.replace(task.getSelectionOffset(), task.getPreviousDisplayLength(), finalText);

        // Add all annotations after text modifications are complete
        for (TextDiff diff : currentDiffs) {
            Position position = new Position(diff.offset(), diff.length());
            String annotationType = diff.isDeletion() ? annotationDeleted : annotationAdded;
            String annotationText = diff.isDeletion() ? "Deleted Code" : "Added Code";
            annotationModel.addAnnotation(new Annotation(annotationType, false, annotationText), position);
        }

        // Store rendered text length for proper clearing next iteration
        task.setPreviousDisplayLength(finalText.length());
        task.setPreviousPartialResponse(newCode);
        task.setNumDeletedLines(deletedLines);
        task.setNumAddedLines(insertedLines);
        return true;
    }

    CompletableFuture<Void> handleDecision(final boolean userAcceptedChanges) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        var typeToRemove = (userAcceptedChanges) ? annotationDeleted : annotationAdded;

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

    void restoreState() {
        try {
            final IAnnotationModel annotationModel = task.getEditor().getDocumentProvider().getAnnotationModel(task.getEditor().getEditorInput());
            clearDiffAnnotations(annotationModel);
        } catch (Exception e) {
            Activator.getLogger().error("Failed to restore state in diff manager: " + e.getMessage(), e);
        }

    }

    void endSession() {
        task = null;
    }

    private void setColorPalette(final boolean isDark) {
        this.annotationAdded = "diffAnnotation.added";
        this.annotationDeleted = "diffAnnotation.deleted";
        if (isDark) {
            this.annotationAdded += ".dark";
            this.annotationDeleted += ".dark";
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

        return StringEscapeUtils.unescapeHtml4(s);
    }
}
