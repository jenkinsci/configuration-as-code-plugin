package io.jenkins.plugins.casc.support.credentials;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import hudson.Extension;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;

import javax.annotation.CheckForNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class DirectEntryPrivateKeySourceConfigurator extends BaseConfigurator<DirectEntryPrivateKeySource> {

    @Override
    protected DirectEntryPrivateKeySource instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return new DirectEntryPrivateKeySource(mapping.getScalarValue("privateKey"));
    }

    @Override
    public Class<DirectEntryPrivateKeySource> getTarget() {
        return DirectEntryPrivateKeySource.class;
    }

    @CheckForNull
    @Override
    public CNode describe(DirectEntryPrivateKeySource instance, ConfigurationContext context) throws Exception {
        final Mapping mapping = new Mapping();
        mapping.putIfAbsent("privateKey", new Scalar("****"));
        return mapping;
    }
}
