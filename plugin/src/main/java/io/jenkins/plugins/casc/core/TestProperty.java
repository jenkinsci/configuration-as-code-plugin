package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.labels.LabelAtomProperty;
import hudson.model.labels.LabelAtomPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Test class to be removed
 *
 */
public class TestProperty extends LabelAtomProperty {

    public final int value;

    @DataBoundConstructor
    public TestProperty(int value) {
        this.value = value;
    }

    @Extension
    public static class DescriptorImpl extends LabelAtomPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "A simple value";
        }
    }
}

