package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Set;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;

@Extension
public class OrganizationFolderItemConfigurator extends BaseConfigurator<OrganizationFolder>
        implements ItemConfigurator<OrganizationFolder> {

    @Override
    @NonNull
    public String getName() {
        return "organizationFolder";
    }

    @Override
    public Class<OrganizationFolder> getTarget() {
        return OrganizationFolder.class;
    }

    @Override
    @NonNull
    public Set<Attribute<OrganizationFolder, ?>> describe() {
        Set<Attribute<OrganizationFolder, ?>> attributes = super.describe();

        attributes.removeIf(attribute -> attribute.getName().equals("displayNameOrNull"));
        attributes.add(
                new Attribute<OrganizationFolder, String>("name", String.class).getter(OrganizationFolder::getName));

        return attributes;
    }

    @Override
    protected OrganizationFolder instance(Mapping mapping, ConfigurationContext context) {
        throw new UnsupportedOperationException("Organization folders must be created with a name");
    }

    @Override
    public OrganizationFolder configure(String name, CNode config, ConfigurationContext context)
            throws ConfiguratorException {
        try {
            Jenkins jenkins = Jenkins.get();
            hudson.model.TopLevelItem existingItem = jenkins.getItem(name);
            OrganizationFolder folder;

            if (existingItem == null) {
                folder = jenkins.createProject(OrganizationFolder.class, name);
            } else if (existingItem instanceof OrganizationFolder) {
                folder = (OrganizationFolder) existingItem;
            } else {
                throw new ConfiguratorException("An item named '" + name
                        + "' already exists but is not an OrganizationFolder (it is a "
                        + existingItem.getClass().getName() + ").");
            }

            Mapping mapping = config.asMapping();
            configure(mapping, folder, false, context);
            folder.save();

            folder.scheduleBuild(new hudson.model.Cause() {
                @Override
                public String getShortDescription() {
                    return "Triggered by Jenkins Configuration as Code";
                }
            });

            return folder;

        } catch (ConfiguratorException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConfiguratorException("Failed to configure organization folder: " + name, e);
        }
    }
}
