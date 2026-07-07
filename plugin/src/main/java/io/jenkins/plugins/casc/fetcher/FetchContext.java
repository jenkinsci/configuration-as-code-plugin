package io.jenkins.plugins.casc.fetcher;

import io.jenkins.plugins.casc.yaml.YamlSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FetchContext implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(FetchContext.class.getName());

    private final List<FetchResult> results = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private final List<YamlSource> yamlSources = new ArrayList<>();

    public void add(FetchResult result) {
        if (result == null) return;

        this.results.add(result);
        List<ResolvedYaml> items = new ArrayList<>(result.items());

        items.sort(java.util.Comparator.comparing(ResolvedYaml::relativePath));

        for (ResolvedYaml item : items) {
            yamlSources.add(YamlSource.of(item));
        }
    }

    @SuppressWarnings("rawtypes")
    public List<YamlSource> getSources() {
        return yamlSources;
    }

    @Override
    public void close() {
        for (FetchResult result : results) {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to clean up JCasC fetch result", e);
            }
        }
    }
}
