package io.jenkins.plugins.casc.history;

import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.plugins.casc.CasCReloadListener;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.springframework.security.core.Authentication;

@Extension
@SuppressWarnings("unused")
public class CasCHistoryRecorder implements CasCReloadListener {

    private static final Logger LOGGER = Logger.getLogger(CasCHistoryRecorder.class.getName());

    @Override
    public void onConfigurationReloaded() {
        LOGGER.fine("CasC reload detected. Queuing async capture of current YAML state...");

        final Authentication auth = Jenkins.getAuthentication2();
        final String triggeredBy = auth.getName();

        Timer.get().submit(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ConfigurationAsCode.get().export(out);
                String currentYaml = out.toString(StandardCharsets.UTF_8);

                CasCHistoryBackend backend = ExtensionList.lookupFirst(CasCHistoryBackend.class);
                backend.save(currentYaml, triggeredBy);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to capture CasC history asynchronously", e);
            }
        });
    }
}
