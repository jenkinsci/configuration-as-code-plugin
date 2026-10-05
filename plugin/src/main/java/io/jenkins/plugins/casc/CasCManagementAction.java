package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.security.Permission;
import jenkins.model.Jenkins;

public interface CasCManagementAction extends ExtensionPoint {

    @CheckForNull
    @SuppressWarnings("unused")
    String getIconFileName();

    String getDisplayName();

    String getUrlName();

    default boolean requiresPost() {
        return true;
    }

    @CheckForNull
    default String getOverridesAction() {
        return null;
    }

    default Permission getRequiredPermission() {
        return Jenkins.MANAGE;
    }

    static ExtensionList<CasCManagementAction> all() {
        return ExtensionList.lookup(CasCManagementAction.class);
    }
}
