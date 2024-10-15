package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QInvocationSessionTest.class})
public class QInvocationSessionTest {

    private final static ITextEditor mockEditor = mock(ITextEditor.class);
    private static PreferenceStoreUtil preferenceStoreUtil;

    @BeforeClass
    public static void setUp() throws Exception {
        preferenceStoreUtil = PowerMockito.mock(PreferenceStoreUtil.class);
        PowerMockito.whenNew(PreferenceStoreUtil.class).withAnyArguments().thenReturn(preferenceStoreUtil);
        when(preferenceStoreUtil.getBoolean(anyString(), anyBoolean())).thenReturn(true);
    }

    @Test
    void testSessionStart() {
        PreferenceStoreUtil prefStoreUtil = new PreferenceStoreUtil("");
        System.out.println("hello");
        assertEquals(true, false);
    }
    
}
