package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator {

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins configure(Object c) throws Exception {
        Map config = (Map) c;
        Jenkins jenkins = Jenkins.getInstance();

        configure(config, jenkins);
        return jenkins;
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        attributes.add(new PersistedListAttribute<Cloud>("clouds", Jenkins.getInstance().clouds, Cloud.class));

        return attributes;
    }

    @Override
    public String getName() {
        return "jenkins";
    }
}
