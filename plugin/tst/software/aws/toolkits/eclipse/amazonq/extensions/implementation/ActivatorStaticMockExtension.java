// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.implementation;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.extensions.api.StaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.LoginService;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.TelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.CodeReferenceLoggingService;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

public final class ActivatorStaticMockExtension extends StaticMockExtension<Activator>
        implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    private boolean isSetup = false;
    private MockedStatic<Activator> activatorStaticMock = null;

    @Override
    public MockedStatic<Activator> getStaticMock() {
        return activatorStaticMock;
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        if (isSetup) {
            return;
        }

        LoggingService loggingServiceMock = Mockito.mock(LoggingService.class);
        TelemetryService telemetryServiceMock = Mockito.mock(TelemetryService.class);
        Activator activatorMock = Mockito.mock(Activator.class);
        LspProvider lspProviderMock = Mockito.mock(LspProvider.class);
        LoginService loginServiceMock = Mockito.mock(LoginService.class);
        PluginStore pluginStoreMock = Mockito.mock(PluginStore.class);
        CodeReferenceLoggingService codeReferenceLoggingServiceMock = Mockito.mock(CodeReferenceLoggingService.class);
        EventBroker eventBrokerMock = Mockito.mock(EventBroker.class);

        IPreferenceStore mockPreferenceStore = Mockito.mock(IPreferenceStore.class);
        when(activatorMock.getPreferenceStore()).thenReturn(mockPreferenceStore);

        activatorStaticMock.when(Activator::getLogger).thenReturn(loggingServiceMock);
        activatorStaticMock.when(Activator::getTelemetryService).thenReturn(telemetryServiceMock);
        activatorStaticMock.when(Activator::getDefault).thenReturn(activatorMock);
        activatorStaticMock.when(Activator::getLspProvider).thenReturn(lspProviderMock);
        activatorStaticMock.when(Activator::getLoginService).thenReturn(loginServiceMock);
        activatorStaticMock.when(Activator::getPluginStore).thenReturn(pluginStoreMock);
        activatorStaticMock.when(Activator::getCodeReferenceLoggingService).thenReturn(codeReferenceLoggingServiceMock);
        activatorStaticMock.when(Activator::getEventBroker).thenReturn(eventBrokerMock);

        Map<Class<?>, Object> newMocksMap = Map.of(
                LoggingService.class, loggingServiceMock,
                TelemetryService.class, telemetryServiceMock,
                Activator.class, activatorMock,
                LspProvider.class, lspProviderMock,
                LoginService.class, loginServiceMock,
                PluginStore.class, pluginStoreMock,
                CodeReferenceLoggingService.class, codeReferenceLoggingServiceMock,
                EventBroker.class, eventBrokerMock
        );
        setMocksMap(newMocksMap);
        isSetup = true;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (activatorStaticMock != null) {
            activatorStaticMock.close();
        }
        activatorStaticMock = mockStatic(Activator.class);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        if (activatorStaticMock != null) {
            activatorStaticMock.close();
            activatorStaticMock = null;
        }
    }

}
