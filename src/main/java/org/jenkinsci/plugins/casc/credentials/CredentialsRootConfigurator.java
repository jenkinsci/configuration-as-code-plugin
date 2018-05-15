package org.jenkinsci.plugins.casc.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.MultivaluedAttribute;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
public class CredentialsRootConfigurator extends Configurator<CredentialsStore> implements RootElementConfigurator<CredentialsStore> {

    private final static Logger logger = Logger.getLogger(CredentialsRootConfigurator.class.getName());


    @Override
    public String getName() {
        return "credentials";
    }

    @Override
    public Class<CredentialsStore> getTarget() {
        return CredentialsStore.class;
    }

    @Override
    public CredentialsStore configure(CNode config) throws ConfiguratorException {
        Mapping map = config.asMapping();
        final Sequence system = map.get("system").asSequence();

        final SystemCredentialsProvider provider = SystemCredentialsProvider.getInstance();
        final Map<Domain, List<Credentials>> target = provider.getDomainCredentialsMap();
        target.clear();

        final Configurator<DomainWithCredentials> c = Configurator.lookup(DomainWithCredentials.class);
        for (CNode cNode : system) {
            final DomainWithCredentials domainWithCredentials = c.configure(cNode);
            target.put(domainWithCredentials.domain, domainWithCredentials.credentials);
        }

        // provider.getStore() is unfortunately private
        final SystemCredentialsProvider.ProviderImpl p = Jenkins.getInstance().getExtensionList(SystemCredentialsProvider.ProviderImpl.class).get(0);
        final CredentialsStore store = p.getStore(Jenkins.getInstance());
        if (store == null) throw new IllegalStateException("SystemCredentialsProvider.getStore returned null");
        return store;
    }

    @Override
    @SuppressFBWarnings(value="DM_NEW_FOR_GETCLASS", justification="one can't get a parameterized type .class")
    public Set<Attribute> describe() {
        return Collections.singleton(new MultivaluedAttribute("system", DomainWithCredentials.class));
    }

    @CheckForNull
    @Override
    public CNode describe(CredentialsStore instance) {
        // FIXME
        return null;
    }

    public static class DomainWithCredentials {

        private Domain domain = Domain.global();
        private List<Credentials> credentials;

        @DataBoundConstructor
        public DomainWithCredentials(List<Credentials> credentials) {
            this.credentials = credentials;
        }

        @DataBoundSetter
        public void setDomain(Domain domain) {
            this.domain = domain;
        }
    }
}
