package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.security.Permission;
import jenkins.model.Jenkins;

public interface CasCManagementAction extends Action, ExtensionPoint {

    default boolean requiresPost() {
        return true;
    }

    default Permission getRequiredPermission() {
        return Jenkins.MANAGE;
    }

    static ExtensionList<CasCManagementAction> all() {
        return ExtensionList.lookup(CasCManagementAction.class);
    }
}
