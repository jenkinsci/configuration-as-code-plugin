package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.model.Descriptor;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.jenkinsci.plugins.casc.Attribute.Setter.NOP;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class GlobalConfigurationCategoryConfigurator extends BaseConfigurator<GlobalConfigurationCategory> implements RootElementConfigurator<GlobalConfigurationCategory> {

    private final static Logger logger = Logger.getLogger(GlobalConfigurationCategoryConfigurator.class.getName());

    private final GlobalConfigurationCategory category;

    public GlobalConfigurationCategoryConfigurator(GlobalConfigurationCategory category) {
        this.category = category;
    }

    @Override
    public String getName() {
        final Class c = category.getClass();
        final Symbol symbol = (Symbol) c.getAnnotation(Symbol.class);
        if (symbol != null) return symbol.value()[0];

        String name = c.getSimpleName();
        name = StringUtils.remove(name, "Global");
        name = StringUtils.remove(name, "Configuration");
        name = StringUtils.remove(name, "Category");
        return name.toLowerCase();
    }

    @Override
    public Class getTarget() {
        return category.getClass();
    }

    @Override
    public GlobalConfigurationCategory getTargetComponent() {
        return category;
    }

    @Override
    protected GlobalConfigurationCategory instance(Mapping mapping) {
        return category;
    }

    @Override
    public Set<Attribute> describe() {
        return Jenkins.getInstance().getExtensionList(Descriptor.class).stream()
                .filter(d -> d.getCategory() == category)
                .filter(d -> d.getGlobalConfigPage() != null)
                .map(d -> new DescriptorConfigurator(d))
                .map(c -> new Attribute(c.getName(), c.getTarget()).setter(NOP))
                .collect(Collectors.toSet());
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalConfigurationCategory instance) {

        final Mapping mapping = new Mapping();
        Jenkins.getInstance().getExtensionList(Descriptor.class).stream()
            .filter(d -> d.getCategory() == category)
            .filter(d -> d.getGlobalConfigPage() != null)
            .forEach(d -> describe(d, mapping));
        return mapping;
    }

    private void describe(Descriptor d, Mapping mapping) {
        final DescriptorConfigurator c = new DescriptorConfigurator(d);
        try {
            final CNode node = c.describe(d);
            if (node != null) mapping.put(c.getName(), node);
        } catch (Exception e) {
            final StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            final Scalar scalar = new Scalar("FAILED TO EXPORT " + d.getClass().getName() + " : \n" + w.toString());
            mapping.put(c.getName(), scalar);
        }
    }

}
