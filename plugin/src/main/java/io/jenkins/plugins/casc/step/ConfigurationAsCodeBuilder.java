package io.jenkins.plugins.casc.step;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class ConfigurationAsCodeBuilder extends Builder implements SimpleBuildStep {
    private List<String> targets;

    @DataBoundConstructor
    public ConfigurationAsCodeBuilder() {
        this.targets = Collections.emptyList();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        ConfigurationAsCode c = ConfigurationAsCode.get();
        c.configure(targets);
    }

    public List<String> getTargets() {
        return targets;
    }

    @DataBoundSetter
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    @Extension
    @Symbol("configurationAsCode")
    public static class DescriptorImpl  extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Process Configuration As Code";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
