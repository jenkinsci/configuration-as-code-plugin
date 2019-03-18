package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import static io.jenkins.plugins.casc.Attribute.noop;


/**
 * TODO would  not be required if UpdateSite had a DataBoundConstructor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class UpdateSiteConfigurator extends BaseConfigurator<UpdateSite> {

    @Override
    public Class<UpdateSite> getTarget() {
        return UpdateSite.class;
    }

    @Override
    protected UpdateSite instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return new UpdateSite(mapping.getScalarValue("id"), mapping.getScalarValue("url"));
    }

    @Override
    public Set<Attribute<UpdateSite, ?>> describe() {
        // setters are marked as noop, a new instance needs to be created.
        return new HashSet<>(Arrays.asList(
            new Attribute<UpdateSite, String>("id", String.class)
                .getter(UpdateSite::getId)
                .setter( noop() ),
            new Attribute<UpdateSite, String>("url", String.class)
                .getter(UpdateSite::getUrl)
                .setter( noop() )
        ));
    }
}
