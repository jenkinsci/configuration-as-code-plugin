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
import jenkins.security.s2m.AdminWhitelistRule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator {

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());

    //TODO: All fields should be rather replaced by another setter which throws error (error propagation)
    // https://github.com/jenkinsci/configuration-as-code-plugin/issues/63
    public static final Attribute.Setter NOOP = (target, attribute, value) -> {
        // Nop
        LOGGER.log(Level.WARNING, "Ignoring attribute {0} for Jenkins instance. No setter", attribute.name);
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

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute("remotingSecurity", AdminWhitelistRule.class).setter(NOOP));

        return attributes;
    }


    @Override
    public String getName() {
        return "jenkins";
    }
}
