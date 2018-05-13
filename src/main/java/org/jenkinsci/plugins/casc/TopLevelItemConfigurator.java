package org.jenkinsci.plugins.casc;

import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import java.util.Map;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class TopLevelItemConfigurator extends BaseConfigurator<TopLevelItem> {

    private final Class<TopLevelItem> target;

    public TopLevelItemConfigurator(Class<TopLevelItem> target) {
        this.target = target;
    }

    @Override
    public Class<TopLevelItem> getTarget() {
        return target;
    }

    @Override
    public TopLevelItem configure(CNode c) throws ConfiguratorException {

        Mapping config = c.asMapping();

        final Jenkins jenkins = Jenkins.getInstance();
        final TopLevelItemDescriptor descriptor = (TopLevelItemDescriptor) jenkins.getDescriptorOrDie(target);
        final String name = config.remove("name").asScalar().toString();
        final TopLevelItem item = descriptor.newInstance(jenkins, name);
        configure(config, item);
        return item;
    }
}
