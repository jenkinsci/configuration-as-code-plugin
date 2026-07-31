package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.io.IOException;
import jenkins.model.Jenkins;

@Extension
public class FreestyleItemConfigurator implements ItemConfigurator<FreeStyleProject> {

    @Override
    public String getName() {
        return "freestyle";
    }

    @Override
    public Class<FreeStyleProject> getTarget() {
        return FreeStyleProject.class;
    }

    @Override
    public FreeStyleProject configure(String name, CNode config, ConfigurationContext context)
            throws ConfiguratorException {
        try {
            Jenkins jenkins = Jenkins.get();
            FreeStyleProject job = (FreeStyleProject) jenkins.getItem(name);

            if (job == null) {
                job = jenkins.createProject(FreeStyleProject.class, name);
            }

            Mapping mapping = config.asMapping();

            if (mapping.containsKey("description")) {
                job.setDescription(mapping.getScalarValue("description"));
            }

            if (mapping.containsKey("displayName")) {
                job.setDisplayName(mapping.getScalarValue("displayName"));
            }

            job.save();
            return job;

        } catch (IOException e) {
            throw new ConfiguratorException("Failed to configure freestyle job: " + name, e);
        }
    }
}
