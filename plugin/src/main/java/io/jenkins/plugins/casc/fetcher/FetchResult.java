package io.jenkins.plugins.casc.fetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FetchResult implements AutoCloseable {

    private final List<ResolvedYaml> items;
    private final List<AutoCloseable> resourcesToClose = new ArrayList<>();
    private final List<Path> pathsToDelete = new ArrayList<>();

    public FetchResult(List<ResolvedYaml> items, AutoCloseable resource) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        if (resource != null) {
            this.resourcesToClose.add(resource);
        }
    }

    public FetchResult(List<ResolvedYaml> items, Path tempDirectory) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        if (tempDirectory != null) {
            this.pathsToDelete.add(tempDirectory);
        }
    }

    public List<ResolvedYaml> items() {
        return items;
    }

    @Override
    public void close() throws IOException {
        for (AutoCloseable resource : resourcesToClose) {
            if (resource == null) continue;
            try {
                resource.close();
            } catch (Exception e) {
                throw new IOException("Failed to close fetched resource", e);
            }
        }

        for (Path tempDirectory : pathsToDelete) {
            if (tempDirectory == null || !Files.exists(tempDirectory)) continue;

            try (var stream = Files.walk(tempDirectory)) {
                List<Path> paths =
                        stream.sorted(java.util.Comparator.reverseOrder()).toList();
                for (Path p : paths) {
                    Files.delete(p);
                }
            } catch (Exception e) {
                throw new IOException("Failed to clean up fetched directory: " + tempDirectory, e);
            }
        }
    }
}
