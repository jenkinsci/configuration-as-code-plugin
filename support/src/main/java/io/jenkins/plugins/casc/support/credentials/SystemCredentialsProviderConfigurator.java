package io.jenkins.plugins.casc.support.credentials;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class SystemCredentialsProviderConfigurator extends BaseConfigurator<SystemCredentialsProvider> {

    @Override
    public Class<SystemCredentialsProvider> getTarget() {
        return SystemCredentialsProvider.class;
    }

    @Override
    protected SystemCredentialsProvider instance(Mapping mapping, ConfigurationContext context) {
        return SystemCredentialsProvider.getInstance();
    }

    @NonNull
    @Override
    public Set<Attribute<SystemCredentialsProvider, ?>> describe() {
        return Collections.singleton(
            new MultivaluedAttribute<SystemCredentialsProvider, DomainCredentials>("domainCredentials", DomainCredentials.class)
                .setter( (target, value) -> target.setDomainCredentialsMap(DomainCredentials.asMap(value)))
        );
    }

    @CheckForNull
    @Override
    public CNode describe(SystemCredentialsProvider instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            mapping.put(attribute.getName(), attribute.describe(instance, context));
        }
        return mapping;
    }
}
