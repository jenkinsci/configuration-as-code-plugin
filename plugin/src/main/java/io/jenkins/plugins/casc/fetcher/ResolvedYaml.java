package io.jenkins.plugins.casc.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class ResolvedYaml {

    private final String relativePath;
    private final StreamSupplier streamSupplier;

    @FunctionalInterface
    public interface StreamSupplier {
        InputStream open() throws IOException;
    }

    public ResolvedYaml(String relativePath, StreamSupplier streamSupplier) {
        this.relativePath = Objects.requireNonNull(relativePath, "relativePath cannot be null");
        this.streamSupplier = Objects.requireNonNull(streamSupplier, "streamSupplier cannot be null");
    }

    public String relativePath() {
        return relativePath;
    }

    public InputStream open() throws IOException {
        return streamSupplier.open();
    }

    @Override
    public String toString() {
        return "ResolvedYaml{relativePath='" + relativePath + "'}";
    }
}
