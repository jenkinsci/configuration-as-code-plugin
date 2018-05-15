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
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import java.util.Collection;
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
    public Jenkins getTargetComponent() {
        return Jenkins.getInstance();
    }

    @Override
    public Jenkins configure(CNode c) throws ConfiguratorException {
        Mapping config = c.asMapping();
        Jenkins jenkins = Jenkins.getInstance();

        configure(config, jenkins);
        return jenkins;
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

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

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<AdminWhitelistRule, Jenkins>("remotingSecurity", AdminWhitelistRule.class).setter(NOOP));

        return attributes;
    }


    @CheckForNull
    @Override
    public CNode describe(Jenkins instance) throws Exception {

        // we can't generate a fresh new Jenkins object as constructor is mixed with init and check for `theInstance` singleton 
        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            mapping.put(attribute.getName(), attribute.describe(instance));
        }
        return mapping;
    }

    @Override
    public String getName() {
        return "jenkins";
    }
}
