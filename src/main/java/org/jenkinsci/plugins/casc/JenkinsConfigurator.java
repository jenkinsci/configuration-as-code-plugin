package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProperty;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.casc.Attribute.NOOP;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator<Jenkins> {

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins configure(Object c) throws ConfiguratorException {
        Map config = (Map) c;
        Jenkins jenkins = Jenkins.getInstance();

        configure(config, jenkins);
        return jenkins;
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        final Jenkins jenkins = Jenkins.getInstance();
        attributes.add(new PersistedListAttribute<Cloud, Jenkins>("clouds", Cloud.class)
            .getter(target -> target.clouds));
        attributes.add(new MultivaluedAttribute<String, Jenkins>("jobs", String.class)
            .setter((target, value) -> {
                JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
                for (String script : value) {
                    new JenkinsDslScriptLoader(mng).runScript(script);
                }
            }));
        attributes.add(new PersistedListAttribute<NodeProperty, Jenkins>("nodeProperties", NodeProperty.class));
        attributes.add(new PersistedListAttribute<NodeProperty, Jenkins>("globalNodeProperties", NodeProperty.class));

        
        // Check for unclassified Descriptors
        final ExtensionList<Descriptor> descriptors = jenkins.getExtensionList(Descriptor.class);
        for (Descriptor descriptor : descriptors) {
            if (descriptor.getGlobalConfigPage() != null && descriptor.getCategory() instanceof GlobalConfigurationCategory.Unclassified) {
                final DescriptorConfigurator configurator = new DescriptorConfigurator(descriptor);
                attributes.add(new Attribute<Descriptor, Jenkins>(configurator.getName(), configurator.getTarget()).setter(NOOP));
            }
        }

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<AdminWhitelistRule, Jenkins>("remotingSecurity", AdminWhitelistRule.class).setter(NOOP));

        return attributes;
    }


    @Override
    public String getName() {
        return "jenkins";
    }
}
