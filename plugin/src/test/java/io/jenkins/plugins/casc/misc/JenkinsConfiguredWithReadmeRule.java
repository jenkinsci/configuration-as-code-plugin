package io.jenkins.plugins.casc.misc;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.StringContains;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author v1v (Victor Martinez)
 */
public class JenkinsConfiguredWithReadmeRule extends JenkinsConfiguredRule {

    static final DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(
        Extensions.ALL
    );
    static final Parser PARSER = Parser.builder(OPTIONS).build();

    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithReadme configuredWithReadme = getConfiguredWithReadme();
        if (Objects.nonNull(configuredWithReadme)) {

            final Class<?> clazz = env.description().getTestClass();
            final String[] resource = configuredWithReadme.value();

            final List<String> configs = Arrays.stream(resource)
                .map(s -> {
                    try {
                        File codeBlockFile = File.createTempFile("integrations", "markdown");
                        InputStream inputStream = clazz.getClassLoader().getResourceAsStream(s);
                        List<String> lines = Arrays.asList(transformFencedCodeBlockFromMarkdownToString(inputStream));
                        Path file = Paths.get(codeBlockFile.getCanonicalPath());
                        Files.write(file, lines, StandardCharsets.UTF_8);
                        return  codeBlockFile.toURI().toString();
                    } catch (IOException e) {
                        throw new AssertionError("Exception when accessing the resources: " + s , e);
                    }
                })
                .collect(Collectors.toList());

            try {
                ConfigurationAsCode.get().configure(configs);
            } catch (Throwable t) {
                if (!configuredWithReadme.expected().isInstance(t)) {
                    throw new AssertionError("Unexpected exception ", t);
                } else {
                    if (!StringUtils.isBlank(configuredWithReadme.message())) {
                        boolean match = new StringContains(configuredWithReadme.message())
                            .matches(t.getMessage());
                        if (!match) {
                            throw new AssertionError(
                                "Exception did not contain the expected string: "
                                    + configuredWithReadme.message() + "\nMessage was:\n" + t
                                    .getMessage());
                        }
                    }
                }
            }
        }
    }

    private ConfiguredWithReadme getConfiguredWithReadme() {
        ConfiguredWithReadme configuredWithReadme = env.description()
            .getAnnotation(ConfiguredWithReadme.class);
        if (Objects.nonNull(configuredWithReadme)) {
            return configuredWithReadme;
        }
        for (Field field : env.description().getTestClass().getFields()) {
            if (field.isAnnotationPresent(ConfiguredWithReadme.class)) {
                int m = field.getModifiers();
                Class<?> clazz = field.getType();
                if (isPublic(m) && isStatic(m) &&
                    clazz.isAssignableFrom(JenkinsConfiguredWithReadmeRule.class)) {
                    configuredWithReadme = field.getAnnotation(ConfiguredWithReadme.class);
                    if (Objects.nonNull(configuredWithReadme)) {
                        return configuredWithReadme;
                    }
                } else {
                    throw new IllegalStateException(
                        "Field must be public static JenkinsConfiguredWithReadmeRule");
                }
            }
        }
        return null;
    }

    private String transformFencedCodeBlockFromMarkdownToString(InputStream markdownContent) throws IOException {
        final MutableDataSet FORMAT_OPTIONS = new MutableDataSet();
        FORMAT_OPTIONS.set(Parser.EXTENSIONS, OPTIONS.get(Parser.EXTENSIONS));
        Reader targetReader = new InputStreamReader(markdownContent);
        Node document = PARSER.parseReader(targetReader);
        TextCollectingVisitor textCollectingVisitor = new TextCollectingVisitor();
        return textCollectingVisitor.collectAndGetText(document.getChildOfType(FencedCodeBlock.class));
    }
}
