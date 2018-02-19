package org.jenkinsci.plugins.casc.misc;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * To load specified config with plugin
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ConfiguredWithCode {

    /**
     * resource path in classpath
     */
    String value();
}
