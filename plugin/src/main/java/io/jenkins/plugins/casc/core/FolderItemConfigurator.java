package io.jenkins.plugins.casc.core;

import com.cloudbees.hudson.plugins.folder.Folder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Set;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class FolderItemConfigurator extends BaseConfigurator<Folder> implements ItemConfigurator<Folder> {

    @Override
    public Class<Folder> getTarget() {
        return Folder.class;
    }

    @Override
    @NonNull
    public String getName() {
        return "folder";
    }

    @Override
    @NonNull
    public Set<Attribute<Folder, ?>> describe() {
        Set<Attribute<Folder, ?>> attributes = super.describe();

        attributes.removeIf(attribute -> attribute.getName().equals("displayNameOrNull"));
        attributes.add(new Attribute<Folder, String>("name", String.class).getter(Folder::getName));

        return attributes;
    }

    @Override
    protected Folder instance(Mapping mapping, ConfigurationContext context) {
        throw new UnsupportedOperationException("Folders must be configured with a name.");
    }

    @Override
    public Folder configure(String name, CNode config, ConfigurationContext context) throws ConfiguratorException {
        try {
            Jenkins jenkins = Jenkins.get();
            TopLevelItem item = jenkins.getItem(name);
            Folder folder;

            if (item == null) {
                folder = jenkins.createProject(Folder.class, name);
            } else if (item instanceof Folder) {
                folder = (Folder) item;
            } else {
                throw new IllegalStateException("An item named '" + name + "' already exists, but it is a "
                        + item.getClass().getSimpleName() + " and not a Folder.");
            }

            if (config != null) {
                super.configure(config.asMapping(), folder, false, context);
            }

            folder.save();
            return folder;
        } catch (ConfiguratorException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConfiguratorException("Failed to configure folder: " + name, e);
        }
    }
}
