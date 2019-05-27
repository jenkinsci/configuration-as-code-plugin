package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.ConfigurationContext.Version;
import org.apache.commons.beanutils.Converter;

public class VersionConverter implements Converter {

    @Override
    public Version convert(Class type, Object value) {
        return Version.of(value.toString());
    }
}
