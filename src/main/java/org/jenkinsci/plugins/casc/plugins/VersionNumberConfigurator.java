package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.util.VersionNumber;
import org.jenkinsci.plugins.casc.BaseConfigurator;

import java.util.Map;

/**
 * Created by mads on 2/2/18.
 */
@Extension
public class VersionNumberConfigurator extends BaseConfigurator<VersionNumber> {

    @Override
    public Class<VersionNumber> getTarget() {
        return VersionNumber.class;
    }

    @Override
    public VersionNumber configure(Object config) throws Exception {
        return new VersionNumber((String)((Map)config).get("version"));
    }
}
