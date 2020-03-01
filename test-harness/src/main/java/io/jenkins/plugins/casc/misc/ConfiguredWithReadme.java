package io.jenkins.plugins.casc.misc;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * To load specified config with plugin from a README file
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
public @interface ConfiguredWithReadme {

    /**
     * Resource path in classpath
     * @return resources to configure the test case with
     */
    String[] value();

    Class<? extends Throwable> expected() default Test.None.class;

    String message() default "";
}
