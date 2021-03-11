package io.jenkins.plugins.casc;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
class IgnoreList {

    private static final Logger LOGGER = Logger.getLogger(IgnoreList.class.getName());

    /**
     * Syntax is: attribute-name space attribute-return-type
     * <p>
     * Enable FINE logging for this class to see all potential candidates
     */
    private static final List<String> IGNORE_LIST = Collections.singletonList(
            "defaultProperties hudson.tools.ToolProperty"
    );

    /**
     * Checks if an attribute should be ignored
     *
     * @param attribute the attribute to check if it should be ignored
     * @return true if the attribute should be ignored
     */
    static boolean isIgnored(Attribute attribute) {
        String attributeRepresentation = attribute.getName() + " " + attribute.getType().getName();

        LOGGER.log(Level.FINE, attributeRepresentation);
        return IGNORE_LIST.contains(attributeRepresentation);
    }
}
