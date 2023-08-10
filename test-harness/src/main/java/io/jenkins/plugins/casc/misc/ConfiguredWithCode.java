package io.jenkins.plugins.casc.misc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.Test;

/**
 * To load specified config with plugin
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ConfiguredWithCode {

    /**
     * Resource path in classpath
     * @return resources to configure the test case with.
     */
    String[] value();

    Class<? extends Throwable> expected() default Test.None.class;

    String message() default "";
}
