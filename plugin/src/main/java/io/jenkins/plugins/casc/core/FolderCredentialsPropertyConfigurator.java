package io.jenkins.plugins.casc.core;

import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class FolderCredentialsPropertyConfigurator extends BaseConfigurator<FolderCredentialsProperty> {

    @Override
    public Class<FolderCredentialsProperty> getTarget() {
        return FolderCredentialsProperty.class;
    }

    @Override
    protected FolderCredentialsProperty instance(Mapping mapping, ConfigurationContext context)
            throws ConfiguratorException {
        return new FolderCredentialsProperty(new DomainCredentials[0]);
    }

    @Override
    @NonNull
    public Set<Attribute<FolderCredentialsProperty, ?>> describe() {
        Set<Attribute<FolderCredentialsProperty, ?>> attributes = new LinkedHashSet<>();

        attributes.add(new MultivaluedAttribute<FolderCredentialsProperty, DomainCredentials>(
                        "domainCredentials", DomainCredentials.class)
                .getter(FolderCredentialsProperty::getDomainCredentials)
                .setter((instance, domainCredentialsCollection) -> {
                    Map<Domain, List<Credentials>> domainCredentialsMap = new HashMap<>();

                    if (domainCredentialsCollection != null) {
                        for (DomainCredentials dc : domainCredentialsCollection) {
                            domainCredentialsMap.put(dc.getDomain(), dc.getCredentials());
                        }
                    }

                    instance.setDomainCredentialsMap(domainCredentialsMap);
                }));

        return attributes;
    }
}
