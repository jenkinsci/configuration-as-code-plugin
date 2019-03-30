package io.jenkins.plugins.casc.support.credentials;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class DirectEntryPrivateKeySourceConfigurator extends DataBoundConfigurator<DirectEntryPrivateKeySource> {

    public DirectEntryPrivateKeySourceConfigurator() {
        super(DirectEntryPrivateKeySource.class);
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
