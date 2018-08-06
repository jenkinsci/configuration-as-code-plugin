package org.jenkinsci.plugins.casc.support.credentials;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import hudson.Extension;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.impl.attributes.MultivaluedAttribute;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

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

    @Nonnull
    @Override
    public Set<Attribute> describe() {
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
