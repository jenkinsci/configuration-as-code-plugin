package org.jenkinsci.plugins.casc.support.matrixauth;

import hudson.Extension;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;

@Extension(optional = true, ordinal = 1)
@Restricted(NoExternalUse.class)
public class ProjectMatrixAuthorizationStrategyConfigurator extends MatrixAuthorizationStrategyConfigurator<ProjectMatrixAuthorizationStrategy> {

    @Override
    public String getName() {
        return "projectMatrix";
    }

    @Override
    public Class<ProjectMatrixAuthorizationStrategy> getTarget() {
        return ProjectMatrixAuthorizationStrategy.class;
    }

    @Override
    public ProjectMatrixAuthorizationStrategy instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return new ProjectMatrixAuthorizationStrategy();
    }

    @CheckForNull
    @Override
    public CNode describe(ProjectMatrixAuthorizationStrategy instance, ConfigurationContext context) throws Exception {
        return compare(instance, new ProjectMatrixAuthorizationStrategy(), context);
    }

}
