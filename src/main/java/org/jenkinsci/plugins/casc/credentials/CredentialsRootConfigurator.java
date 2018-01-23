package org.jenkinsci.plugins.casc.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.Extension;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;

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
public class CredentialsRootConfigurator extends Configurator<CredentialsStore> implements RootElementConfigurator {

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
    public CredentialsStore configure(Object config) throws Exception {
        Map map = (Map) config;
        final Map<?,?> system = (Map) map.get("system");
        final Map<Domain, List<Credentials>> target = SystemCredentialsProvider.getInstance().getDomainCredentialsMap();
        target.clear();

        final Configurator<Domain> domainConfigurator = Configurator.lookup(Domain.class);
        final Configurator<Credentials> credentialsConfigurator = Configurator.lookup(Credentials.class);

        for (Map.Entry dc : system.entrySet()) {
            final Domain domain = domainConfigurator.configure(dc.getKey());
            List values = (List) dc.getValue();
            final List<Credentials> credentials =  new ArrayList<>();
            for (Object value : values) {
                 credentials.add(credentialsConfigurator.configure(value));
            }
            logger.info("Setting "+target.getClass().getCanonicalName()+"#system["+domain.toString()+"] = " + credentials);
            target.put(domain, credentials);
        }
        return null;
    }

    @Override
    @SuppressWarnings(value="DM_NEW_FOR_GETCLASS") // one can't get a parameterized type .class
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute("system", new HashMap<Domain, List<Credentials>>().getClass()));
    }
}
