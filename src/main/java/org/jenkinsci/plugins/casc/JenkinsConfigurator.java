package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.slaves.Cloud;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator {

    public static final Attribute.Setter NOOP = (target, attribute, value) -> {
        // Nop
    };

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

        final Jenkins jenkins = Jenkins.getInstance();
        attributes.add(new PersistedListAttribute<Cloud>("clouds", jenkins.clouds, Cloud.class));
        attributes.add(new Attribute<String>("jobs", String.class).multiple(true).setter((target, attribute, value) -> {
            JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
            for (String script : (List<String>) value) {
                new JenkinsDslScriptLoader(mng).runScript(script);
            }
        }));
        
        // Check for unclassified Descriptors
        final ExtensionList<Descriptor> descriptors = jenkins.getExtensionList(Descriptor.class);
        for (Descriptor descriptor : descriptors) {
            if (descriptor.getGlobalConfigPage() != null && descriptor.getCategory() instanceof GlobalConfigurationCategory.Unclassified) {
                final DescriptorConfigurator configurator = new DescriptorConfigurator(descriptor);
                attributes.add(new Attribute(configurator.getName(), configurator.getTarget()).setter(NOOP));
            }
        }

        return attributes;
    }


    @Override
    public String getName() {
        return "jenkins";
    }
}
