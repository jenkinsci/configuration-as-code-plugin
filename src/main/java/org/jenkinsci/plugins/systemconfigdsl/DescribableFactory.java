package org.jenkinsci.plugins.systemconfigdsl;

import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.structs.describable.DescribableParameter;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Closure delegate that instantiates a data-bindable object
 * via {@link DataBoundConstructor}.
 *
 * <p>
 * Constructor arguments and setter arguments are equally treated in the syntax.
 *
 * @author Kohsuke Kawaguchi
 */
public class DescribableFactory extends PropertyBuilder {
    private final DescribableModel<?> model;

    public DescribableFactory(Class<?> type) {
        super(type);

        model = new DescribableModel<>(type);

        for (DescribableParameter p : model.getParameters()) {
            if (p.isRequired()) // general setters are
                properties.put(p.getName(), new Property(p.getName(), p.getRawType(), null));
        }
    }


    /*package*/ Object instantiate() {
        try {
            Object o = model.instantiate(properties);
            handleSetters(o);
            return o;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate "+type,e);
        }
    }
}
