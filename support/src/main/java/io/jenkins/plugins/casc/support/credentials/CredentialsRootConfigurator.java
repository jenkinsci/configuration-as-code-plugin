package io.jenkins.plugins.casc.support.credentials;

import com.cloudbees.plugins.credentials.GlobalCredentialsConfiguration;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Extension;
import jenkins.model.Jenkins;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class CredentialsRootConfigurator extends BaseConfigurator<GlobalCredentialsConfiguration> implements RootElementConfigurator<GlobalCredentialsConfiguration> {

    private final static Logger logger = Logger.getLogger(CredentialsRootConfigurator.class.getName());

    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    public Class<GlobalCredentialsConfiguration> getTarget() {
        return GlobalCredentialsConfiguration.class;
    }

    @Override
    public GlobalCredentialsConfiguration getTargetComponent(ConfigurationContext context) {
        return GlobalCredentialsConfiguration.all().get(GlobalCredentialsConfiguration.class);
    }

    @Override
    public GlobalCredentialsConfiguration instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<Jenkins, SystemCredentialsProvider>("system", SystemCredentialsProvider.class)
            .getter( t -> SystemCredentialsProvider.getInstance() )
            .setter( Attribute.NOOP ));
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalCredentialsConfiguration instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            mapping.put(attribute.getName(), attribute.describe(Jenkins.getInstance(), context));
        }
        return mapping;
    }

}
