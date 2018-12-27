package io.jenkins.plugins.casc.support.groovy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Sequence;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import groovy.lang.GroovyShell;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * @author <a href="mailto:tomasz.szandala@gmail.com">Tomasz Szandala</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class GroovyScriptCaller implements RootElementConfigurator<Boolean[]> {

    @Override
    public String getName() {
        return "groovy";
    }

    @Override
    public Class getTarget() {
        return Boolean[].class;
    }

    @Override
    public Set<Attribute<Boolean[],?>> describe() {
        return Collections.singleton(new MultivaluedAttribute("", GroovyScriptSource.class));
    }

    @Override
    public Boolean[] getTargetComponent(ConfigurationContext context) {
        return new Boolean[0]; // Doesn't really make sense
    }

    @Override
    public Boolean[] configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        //JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        final Sequence sources = config.asSequence();
        final Configurator<GroovyScriptSource> con = context.lookup(GroovyScriptSource.class);
        List<Boolean> generated = new ArrayList<>();
        for (CNode source : sources) {
            final String script;
            try {
                script = con.configure(source, context).getScript();
            } catch (IOException e) {
                throw new ConfiguratorException(this, "Failed to retrieve Groovy script", e);
            }
            try {
                //Binding binding = new Binding();
                //binding.setVariable("foo", new Integer(2));
                GroovyShell shell = new GroovyShell();
                shell.evaluate(script);
                generated.add(true);

            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }
        return generated.toArray(new Boolean[generated.size()]);
    }

    @Override
    public Boolean[] check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        // Any way to dry-run a Groovy script ?
        return new Boolean[0];
    }

    @Nonnull
    @Override
    public List<Configurator> getConfigurators(ConfigurationContext context) {
        return Collections.singletonList(context.lookup(GroovyScriptSource.class));
    }

    @CheckForNull
    @Override
    public CNode describe(Boolean[] instance, ConfigurationContext context) throws Exception {
        return null;
    }
}
