package io.jenkins.plugins.casc.fetcher;

import hudson.Extension;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension(ordinal = -100)
public class LocalFileSystemFetcher implements CasCConfigFetcher {

    private static final String YAML_FILES_PATTERN = "glob:**.{yml,yaml,YAML,YML}";

    @Override
    public boolean supports(String location) {
        if (location == null) {
            return false;
        }
        if (location.startsWith("file:")) {
            return true;
        }
        try {
            return Files.exists(Paths.get(location));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FetchResult fetch(String location, FetchCredentials credentials) throws IOException {
        final Path root;
        if (location.startsWith("file:")) {
            try {
                root = Paths.get(URI.create(location));
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid file URI format: " + location, e);
            }
        } else {
            root = Paths.get(location);
        }

        if (!Files.exists(root)) {
            throw new IOException("Invalid configuration: '" + root + "' isn't a valid path.");
        }

        if (Files.isRegularFile(root) && Files.isReadable(root)) {
            Path fileNamePath = root.getFileName();
            String fileName = fileNamePath != null ? fileNamePath.toString() : root.toString();
            ResolvedYaml resolved = new ResolvedYaml(fileName, () -> Files.newInputStream(root));
            return new FetchResult(Collections.singletonList(resolved), (AutoCloseable) null);
        }

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);
        try (Stream<Path> stream = Files.find(
                root,
                Integer.MAX_VALUE,
                (next, attrs) -> !attrs.isDirectory() && matcher.matches(next),
                FileVisitOption.FOLLOW_LINKS)) {

            List<ResolvedYaml> items = stream.map(path -> {
                        String relativePath = root.relativize(path).toString().replace('\\', '/');
                        return new ResolvedYaml(relativePath, () -> Files.newInputStream(path));
                    })
                    .collect(Collectors.toList());

            return new FetchResult(items, (AutoCloseable) null);
        }
    }
}
