package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.beanutils.PropertyUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;

import java.awt.*;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator {

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins configure(Object c) throws Exception {
        Map config = (Map) c;
        Jenkins jenkins = Jenkins.getInstance();

        configure(config, jenkins);
        return jenkins;
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        attributes.add(new Attribute<Jenkins>("jobs", TopLevelItem.class) {
            @Override
            public void setValue(Jenkins jenkins, Object value) throws Exception {
                List<TopLevelItem> jobs = (List<TopLevelItem>) value;
                // FIXME not pleasant we have to re-implement jenkins.createProject logic here
                for (TopLevelItem item : jobs) {
                    final String name = item.getName();
                    if (jenkins.getItem(name) == null) {
                        item.onCreatedFromScratch();
                        item.save();
                        jenkins.add(item, name);
                    } else {
                        // FIXME re-configure ? remove/replace ?
                    }
                }
                Jenkins.getInstance().rebuildDependencyGraphAsync();
            }
        }.multiple(true));

        attributes.add(new Attribute<Jenkins>("clouds", Cloud.class) {
            @Override
            public void setValue(Jenkins jenkins, Object value) throws Exception {
                List<Cloud> clouds = (List<Cloud>) value;
                for (Cloud cloud : clouds) {
                    if (jenkins.getCloud(cloud.name) == null) {
                        jenkins.clouds.add(cloud);
                    } else {
                        // FIXME re-configure ? remove/replace ?
                    }
                }
            }
        }.multiple(true));


        return attributes;
    }

    @Override
    public String getName() {
        return "jenkins";
    }
}
