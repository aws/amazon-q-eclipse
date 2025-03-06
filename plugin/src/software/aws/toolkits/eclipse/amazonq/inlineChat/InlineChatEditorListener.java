package software.aws.toolkits.eclipse.amazonq.inlineChat;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public class InlineChatEditorListener implements IPartListener2, EventObserver<AuthState> {
    private static InlineChatEditorListener instance;
    private final InlineChatUIManager uiManager;
    private final ThemeDetector themeDetector;
    private final Disposable authStateSubscription;

    private IWorkbenchWindow window;
    private Runnable pendingPromptUpdate;
    private ISelectionChangedListener currentSelectionListener;
    private PaintListener currentPaintListener;
    private ITextViewer currentViewer;
    private final boolean isDarkTheme;
    private boolean isLoggedIn = Activator.getLoginService().getAuthState().isLoggedIn();

    private final String INLINE_CHAT_TIP_MESSAGE = "Amazon Q: ⌘ + I";
    private static final int SELECTION_DELAY_MS = 500;


    private InlineChatEditorListener() {
        // Prevent instantiation
        this.themeDetector = new ThemeDetector();
        this.isDarkTheme = themeDetector.isDarkTheme();
        this.uiManager = InlineChatUIManager.getInstance();
        this.authStateSubscription = Activator.getEventBroker().subscribe(AuthState.class, this);

    }

    @Override
    public void onEvent(final AuthState authState) {
        isLoggedIn = authState.isLoggedIn();
    }

    public static InlineChatEditorListener getInstance() {
        if (instance == null) {
            instance = new InlineChatEditorListener();
        }
        return instance;
    }

    public void initialize() {
        Display.getDefault().asyncExec(() -> {
            try {
                window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window != null) {
                    window.getActivePage().addPartListener(this);

                    // Ensure that prompt works for existing editor
                    IEditorPart activeEditor = window.getActivePage().getActiveEditor();
                    if (activeEditor instanceof ITextEditor) {
                        partActivated(window.getActivePage().getActivePartReference());
                    }
                }
            } catch (Exception e) {
                Activator.getLogger().error("Failed to initialize inline chat editor listener: " + e.getMessage(), e);
            }
        });
    }

    public void closePrompt() {
        removeCurrentPaintListener();
    }

    public void dispose() {
        authStateSubscription.dispose();
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) partRef.getPart(false);
            try {
                attachSelectionListener(editor);
            } catch (Exception e) {
                Activator.getLogger().error("Failed in process of attaching selection listener: " + e.getMessage(), e);
            }

        }
    }

    @Override
    public void partDeactivated(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) partRef.getPart(false);
            removeSelectionListener(editor);
        }
    }

    private void showPrompt(final ITextEditor editor, final ITextSelection selection) {
        // Always close existing prompt first
        closePrompt();

        Display.getDefault().asyncExec(() -> {
            try {
                // Check if we still have a valid selection before showing prompt
                if (editor.getSelectionProvider().getSelection() instanceof ITextSelection) {
                    ITextSelection currentSelection = (ITextSelection) editor.getSelectionProvider().getSelection();

                    // Only show if selection hasn't changed
                    if (currentSelection.equals(selection)) {
                        currentViewer = editor.getAdapter(ITextViewer.class);
                        var widget = currentViewer.getTextWidget();
                        currentPaintListener = uiManager.createPaintListenerPrompt(widget, selection.getOffset(), INLINE_CHAT_TIP_MESSAGE, isDarkTheme);

                        widget.addPaintListener(currentPaintListener);
                        widget.redraw();
                    }
                }
            } catch (Exception e) {
                Activator.getLogger().error("Failed to show prompt: " + e.getMessage(), e);
            }
        });
    }

    private void attachSelectionListener(final ITextEditor editor) {

        // Remove any existing listeners
        removeSelectionListener(editor);

        ISelectionProvider selectionProvider = editor.getSelectionProvider();
        if (selectionProvider == null) {
            return;
        }

        currentSelectionListener = event -> {
            if (!isLoggedIn) {
                return;
            }
            // Cancel any pending prompt updates
            if (pendingPromptUpdate != null) {
                Display.getDefault().timerExec(-1, pendingPromptUpdate);
                pendingPromptUpdate = null;
            }

            if (event.getSelection() instanceof ITextSelection) {
                ITextSelection selection = (ITextSelection) event.getSelection();

                if (selection.getLength() > 0) {
                    int startLine = selection.getStartLine();
                    int endLine = selection.getEndLine();

                    // Only show if 2+ lines are selected
                    if (endLine - startLine >= 1) {

                        // Delay prompt to give user time to complete selection
                        pendingPromptUpdate = () -> {
                            showPrompt(editor, selection);
                            pendingPromptUpdate = null;
                        };
                        Display.getDefault().timerExec(SELECTION_DELAY_MS, pendingPromptUpdate);
                    } else {
                        closePrompt();
                    }
                } else {
                    closePrompt();
                }
            }
        };
        selectionProvider.addSelectionChangedListener(currentSelectionListener);
    }

    private void removeCurrentPaintListener() {
        try {
            if (currentViewer != null && !currentViewer.getTextWidget().isDisposed() && currentPaintListener != null) {
                Display.getDefault().syncExec(() -> {
                    currentViewer.getTextWidget().removePaintListener(currentPaintListener);
                    currentViewer.getTextWidget().redraw();
                    currentPaintListener = null;
                });
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to remove paint listener: " + e.getMessage(), e);
        }

    }

    private void removeSelectionListener(final ITextEditor editor) {
        if (currentSelectionListener != null && editor.getSelectionProvider() != null) {
            editor.getSelectionProvider().removeSelectionChangedListener(currentSelectionListener);
            currentSelectionListener = null;
        }
    }
}
