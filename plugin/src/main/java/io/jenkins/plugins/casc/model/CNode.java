package io.jenkins.plugins.casc.model;

import io.jenkins.plugins.casc.ConfiguratorException;

/**
 * A configuration Node in yaml tree.
 * (We didn't used <em>Node</em> as class name to avoid collision with commonly used Jenkins class hudson.model.Node
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface CNode extends Cloneable {

    enum Type {
        MAPPING,
        SEQUENCE,
        SCALAR
    }

    Type getType();

    default Mapping asMapping() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Mapping" + getSourceLocation());
    }

    default Sequence asSequence() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Sequence" + getSourceLocation());
    }

    default Scalar asScalar() throws ConfiguratorException {
        throw new ConfiguratorException("Item isn't a Scalar" + getSourceLocation());
    }

    /** @deprecated sensitive data are identified based on target attribute being a ${@link hudson.util.Secret} */
    @Deprecated
    default boolean isSensitiveData() {
        return false;
    }

    /**
     * Indicates if the field should be included when describing even if empty
     * @return false by default
     */
    default boolean isPrintableWhenEmpty() {
        return false;
    }

    /**
     * Indicate the source (file, line number) this specific configuration node comes from.
     * This is used to offer relevant diagnostic messages
     */
    Source getSource();

    default String getSourceLocation() {
        Source s = getSource();
        return s != null ? " " + s : "";
    }

    CNode clone();
}
