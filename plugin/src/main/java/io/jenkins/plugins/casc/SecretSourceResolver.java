package io.jenkins.plugins.casc;

import static io.vavr.API.unchecked;

import org.bigtesting.interpolatd.Interpolator;

import java.util.Optional;
import java.util.stream.Stream;

public class SecretSourceResolver {
    private static final String enclosedBy = "${";
    private static final String enclosedIn = "}";
    private static final String escapedWith = "^";
    private static final String defaultDelimiter = ":-";

    public static String resolve(ConfigurationContext context, String toInterpolate) {
        return interpolator(context).interpolate(toInterpolate, "");
    }

    private static Interpolator<String> interpolator(ConfigurationContext context) {
        Interpolator<String> interpolator = new Interpolator<>();
        interpolator.when().enclosedBy(enclosedBy).and(enclosedIn).handleWith((captured, argument) -> handle(context, captured));
        interpolator.escapeWith(escapedWith);
        return interpolator;
    }

    private static String handle(ConfigurationContext context, String captured) {
        int limit = 2;
        String[] split = captured.split(defaultDelimiter, limit);
        String reveal = split[0];
        Optional<String> defaultValue = Optional.empty();
        if (split.length == limit) {
            defaultValue = Optional.ofNullable(split[1]);
        }
        return reveal(context, reveal)
                .map(Optional::of)
                .orElse(defaultValue)
                .orElse("");
    }

    private static Optional<String> reveal(ConfigurationContext context, String captured) {
        return context.getSecretSources().stream()
                .map(source -> unchecked(() -> source.reveal(captured)).apply())
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
}
