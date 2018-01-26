package org.jenkinsci.plugins.casc;

import hudson.Extension;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * {@link SecretSource} implementation relying on <a href="https://docs.docker.com/engine/swarm/secrets">docker secrets</a>.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class DockerSecretSource extends SecretSource {

    @Override
    public Optional<String> reveal(String secret) throws IOException {
        final File file = new File("/run/secrets/" + secret);
        if (file.exists()) {
            return Optional.of(FileUtils.readFileToString(file));
        }
        return Optional.empty();
    }
}
