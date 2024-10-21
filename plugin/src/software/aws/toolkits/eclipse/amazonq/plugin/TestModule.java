package software.aws.toolkits.eclipse.amazonq.plugin;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;

public final class TestModule implements Module {

    @Override
    public void configure(final Binder binder) {
        binder.bind(TestClassGuice.class).in(Singleton.class);
    }
}
