package io.jenkins.plugins.casc.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EnvVarsRule extends EnvironmentVariables {

    private Class<?> clazz;

    private void setValue(Env env) {
        String value = env.value();
        if (env.name().endsWith("FILE_PATH")) {
            value = new File(clazz.getResource(value).getPath()).getAbsolutePath();
        }
        set(env.name(), value);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        EnvsFromFile configuredWithEnvsFromFile = description.getAnnotation(EnvsFromFile.class);
        clazz = description.getTestClass();
        if (Objects.nonNull(configuredWithEnvsFromFile)) {

            final String[] resource = configuredWithEnvsFromFile.value();

            final List<String> envFiles = Arrays.stream(resource)
                .map(s -> Paths.get(System.getProperty("java.io.tmpdir"), s).toString())
                .collect(Collectors.toList());

            Properties properties = new Properties();
            for (String file : envFiles) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            properties.forEach((key, value) -> set(String.valueOf(key), String.valueOf(value)));
        }
        Envs configuredWithEnvs = description.getAnnotation(Envs.class);
        if (Objects.nonNull(configuredWithEnvs)) {
            List<Env> envs = Arrays.asList(configuredWithEnvs.value());
            envs.forEach(this::setValue);
        }
        return super.apply(base, description);
    }
}
