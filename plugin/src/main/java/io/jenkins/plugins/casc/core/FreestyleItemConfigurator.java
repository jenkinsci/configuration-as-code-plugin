package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.triggers.Trigger;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collection;
import java.util.Set;
import jenkins.model.Jenkins;

@Extension
public class FreestyleItemConfigurator extends BaseConfigurator<FreeStyleProject>
        implements ItemConfigurator<FreeStyleProject> {

    @Override
    @NonNull
    public String getName() {
        return "freestyle";
    }

    @Override
    public Class<FreeStyleProject> getTarget() {
        return FreeStyleProject.class;
    }

    @Override
    @NonNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<Attribute<FreeStyleProject, ?>> describe() {
        Set<Attribute<FreeStyleProject, ?>> attributes = super.describe();

        for (Attribute<FreeStyleProject, ?> attribute : attributes) {
            switch (attribute.getName()) {
                case "buildersList":
                    attribute.preferredName("builders");
                    break;
                case "publishersList":
                    attribute.preferredName("publishers");
                    break;
                case "buildWrappersList":
                    attribute.preferredName("buildWrappers");
                    break;
            }
        }

        Attribute properties = new Attribute<FreeStyleProject, Object>("properties", JobProperty.class)
                .multiple(true)
                .getter(Job::getAllProperties)
                .setter((job, props) -> {
                    try {
                        if (props instanceof Collection) {
                            for (JobProperty p : (Collection<JobProperty>) props) {
                                job.removeProperty(p.getClass());
                                job.addProperty(p);
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply properties", e);
                    }
                });
        attributes.add(properties);

        Attribute triggers = new Attribute<FreeStyleProject, Object>("triggers", Trigger.class)
                .multiple(true)
                .getter(job -> job.getTriggers().values())
                .setter((job, trigs) -> {
                    try {
                        if (trigs instanceof Collection) {
                            for (Trigger t : (Collection<Trigger>) trigs) {
                                job.removeTrigger(t.getDescriptor());
                                job.addTrigger(t);
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply triggers", e);
                    }
                });
        attributes.add(triggers);

        return attributes;
    }

    @Override
    protected FreeStyleProject instance(Mapping mapping, ConfigurationContext context) {
        throw new UnsupportedOperationException("Freestyle projects must be created with a name");
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

            configure(mapping, job, false, context);

            job.save();
            return job;

        } catch (ConfiguratorException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConfiguratorException("Failed to configure freestyle job: " + name, e);
        }
    }
}
