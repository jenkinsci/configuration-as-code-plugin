package org.jenkinsci.plugins.casc.model;

import org.jenkinsci.plugins.casc.ConfiguratorException;

/**
 * A configuration Node in yaml tree.
 * (We didn't used <em>Node</em> as class name to avoid collision with commonly used Jenkins class hudson.model.Node
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface CNode {

    enum Type { MAPPING, SEQUENCE, SCALAR }

    Type getType();

    default Mapping asMapping() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Mapping");
    }

    default Sequence asSequence() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Sequence");
    }

    default Scalar asScalar() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Scalar");
    }

    default boolean isSensitiveData() { return false; }

    /**
     * Indicate the source (file, line number) this specific configuration node comes from.
     * This is used to offer relevant diagnostic messages
     */
    Source getSource();

}
