package io.jenkins.plugins.casc;

import io.vavr.Tuple;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bigtesting.interpolatd.Interpolator;

import static io.vavr.API.unchecked;

public class SecretSourceResolver {
    private static final String enclosedBy = "${";
    private static final String enclosedIn = "}";
    private static final String escapedWith = "^";
    private static final String defaultDelimiter = ":-";

    private static final Logger LOGGER = Logger.getLogger(SecretSourceResolver.class.getName());

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
        String[] split = captured.split(defaultDelimiter, 2);
        return Tuple.of(split[0], Try.of(() -> split[1]).toJavaOptional()).apply(
            (toReveal, defaultValue) -> reveal(context, toReveal)
                .map(Optional::of)
                .orElse(defaultValue)
                .orElseGet(() -> handleUndefinedVariable(captured)));
    }

    private static String handleUndefinedVariable(String captured) {
        LOGGER.log(Level.WARNING, "Configuration import: Found unresolved variable {0}. " +
                "Will default to empty string", captured);
        return "";
    }

    private static Optional<String> reveal(ConfigurationContext context, String captured) {
        return context.getSecretSources().stream()
                .map(source -> unchecked(() -> source.reveal(captured)).apply())
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
}
