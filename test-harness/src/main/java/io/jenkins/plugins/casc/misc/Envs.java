package io.jenkins.plugins.casc.misc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.Test;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Envs {

    Env[] value() default {};

    Class<? extends Throwable> expected() default Test.None.class;

    String message() default "";
}
