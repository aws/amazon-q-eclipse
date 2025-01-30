// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthStateType;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;

public final class ViewRouterTest {

    private PublishSubject<Object> publishSubject;

    private Observable<AuthState> authStateObservable;
    private Observable<LspState> lspStateObservable;

    private ViewRouter viewRouter;
    private EventBroker eventBrokerMock;

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @BeforeEach
    void setupBeforeEach() {
        // ensure event handlers run on same thread
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());

        publishSubject = PublishSubject.create();
        authStateObservable = publishSubject.ofType(AuthState.class);
        lspStateObservable = publishSubject.ofType(LspState.class);

        eventBrokerMock = activatorStaticMockExtension.getMock(EventBroker.class);

        viewRouter = ViewRouter.builder().withAuthStateObservable(authStateObservable)
                .withLspStateObservable(lspStateObservable).build();
    }

    @AfterEach
    public void resetAfterEach() {
        RxJavaPlugins.reset();
    }

    @ParameterizedTest
    @MethodSource("provideStateSource")
    void testActiveViewResolutionBasedOnPluginState(final LspState lspState, final AuthState authState,
            final ViewId activeViewId) {
        publishSubject.onNext(authState);
        publishSubject.onNext(lspState);

        verify(eventBrokerMock).post(activeViewId);
    }

    private static Stream<Arguments> provideStateSource() {
        return Stream.of(Arguments.of(LspState.FAILED, getAuthStateObject(AuthStateType.LOGGED_IN),
                        ViewId.LSP_STARTUP_FAILED_VIEW),
                Arguments.of(LspState.FAILED, getAuthStateObject(AuthStateType.LOGGED_OUT),
                        ViewId.LSP_STARTUP_FAILED_VIEW),
                Arguments.of(LspState.FAILED, getAuthStateObject(AuthStateType.EXPIRED),
                        ViewId.LSP_STARTUP_FAILED_VIEW),
                Arguments.of(LspState.PENDING, getAuthStateObject(AuthStateType.LOGGED_OUT), ViewId.TOOLKIT_LOGIN_VIEW),
                Arguments.of(LspState.ACTIVE, getAuthStateObject(AuthStateType.LOGGED_OUT), ViewId.TOOLKIT_LOGIN_VIEW),
                Arguments.of(LspState.PENDING, getAuthStateObject(AuthStateType.EXPIRED), ViewId.RE_AUTHENTICATE_VIEW),
                Arguments.of(LspState.ACTIVE, getAuthStateObject(AuthStateType.EXPIRED), ViewId.RE_AUTHENTICATE_VIEW),
                Arguments.of(LspState.ACTIVE, getAuthStateObject(AuthStateType.LOGGED_IN), ViewId.CHAT_VIEW));
    }

    private static AuthState getAuthStateObject(final AuthStateType authStateType) {
        return new AuthState(authStateType, null, null, null, null);
    }

}
