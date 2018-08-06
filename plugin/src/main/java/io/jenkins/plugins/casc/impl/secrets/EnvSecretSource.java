package io.jenkins.plugins.casc.impl.secrets;

import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Optional;

@Extension
@Restricted(NoExternalUse.class)
public class EnvSecretSource extends SecretSource {

    @Override
    public Optional<String> reveal(String envKey) {
        Optional<String> returnValue = Optional.empty();
        String config = System.getProperty(envKey, System.getenv(envKey));
        if (config != null) returnValue = Optional.of(config);
        return returnValue;
    }
}
