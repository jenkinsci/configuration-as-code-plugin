package io.jenkins.plugins.casc.support.scriptapproval;

import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:ohad.david@gmail.com">Ohad David</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class ScriptApprovalConfigurator extends BaseConfigurator<ScriptApproval> implements RootElementConfigurator<ScriptApproval> {


    @Override
    public String getName() {
        return "scriptApproval";
    }

    @Override
    public Class<ScriptApproval> getTarget() {
        return ScriptApproval.class;
    }

    @Override
    public Set<Attribute<ScriptApproval,?>> describe() {
        final Set<Attribute<ScriptApproval,?>> describe = new HashSet<>();
        describe.add(new MultivaluedAttribute<ScriptApproval, String>("approvedSignatures", String.class)
            .getter(target ->
                    Arrays.stream(target.getApprovedSignatures())
                    .collect(Collectors.toList()))
            .setter((target, value) -> {
                for (String approval : value) {
                    target.approveSignature(approval);
                }
            }
        ));
        return describe;
    }

    @CheckForNull
    @Override
    public CNode describe(ScriptApproval instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            mapping.put(attribute.getName(), attribute.describe(ScriptApproval.get(), context));
        }
        return mapping;
    }

    @Override
    protected ScriptApproval instance(Mapping mapping, ConfigurationContext context) {
        return ScriptApproval.get();
    }

    @Override
    public ScriptApproval getTargetComponent(ConfigurationContext context) {
        return ScriptApproval.get();
    }
}
