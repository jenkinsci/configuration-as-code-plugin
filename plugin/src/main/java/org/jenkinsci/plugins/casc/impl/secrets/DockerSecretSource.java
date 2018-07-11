package org.jenkinsci.plugins.casc.impl.secrets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.casc.SecretSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * {@link SecretSource} implementation relying on <a href="https://docs.docker.com/engine/swarm/secrets">docker secrets</a>.
 * The path to secret directory can be overridden by setting environment variable <tt>SECRETS</tt>.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(Beta.class)
public class DockerSecretSource extends SecretSource {

    public static final String DOCKER_SECRETS = "/run/secrets/";
    private final File secrets;

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public DockerSecretSource() {
        String s = System.getenv("SECRETS");
        secrets = s != null ? new File(s) : new File(DOCKER_SECRETS);
    }

    @Override
    public Optional<String> reveal(String secret) throws IOException {
        final File file = new File(secrets, secret);
        if (file.exists()) {
            return Optional.of(FileUtils.readFileToString(file).trim());
        }
        return Optional.empty();
    }
}
