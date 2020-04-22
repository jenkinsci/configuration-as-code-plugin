package io.jenkins.plugins.casc.impl.configurators.nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class checks the behaviour of {@link io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator}
 * with {@link Nonnull} {@link List} parameters.
 */
@ParametersAreNonnullByDefault
public class ClassParametersAreNonnullByDefault {
    private List<String> strings;

    @DataBoundConstructor
    public ClassParametersAreNonnullByDefault(@NonNull List<String> strings) {
        this.strings = strings;
    }

    public List<String> getStrings() {
        return strings;
    }
}
