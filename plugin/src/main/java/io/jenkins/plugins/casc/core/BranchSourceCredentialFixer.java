package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Fixes credential initialization issues for MultiBranchProject and OrganizationFolder
 * items created via JCasC/job-dsl.
 *
 * <p>When these items are created programmatically (not through the UI), the credential
 * binding that normally happens during form submission doesn't occur. This causes
 * "Invalid scan credentials" errors when trying to scan repositories.
 *
 * <p>The workaround is to trigger a save operation after the item is created, which
 * properly initializes the credential bindings.
 *
 * <p>See <a href="https://github.com/jenkinsci/configuration-as-code-plugin/issues/2753">Issue #2753</a>
 *
 * @author Configuration as Code Plugin contributors
 */
@Extension
@Restricted(NoExternalUse.class)
public class BranchSourceCredentialFixer extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(BranchSourceCredentialFixer.class.getName());

    // Class names for reflection-based detection (to avoid hard dependency on branch-api)
    private static final String MULTI_BRANCH_PROJECT_CLASS = "jenkins.branch.MultiBranchProject";
    private static final String ORGANIZATION_FOLDER_CLASS = "jenkins.branch.OrganizationFolder";

    /**
     * Flag to track if we're currently within a JCasC configuration cycle.
     * This is set by ConfigurationAsCode before and after configuration.
     */
    private static final ThreadLocal<Boolean> IN_CASC_CONTEXT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Called by ConfigurationAsCode to indicate that JCasC configuration is starting.
     */
    public static void enterCascContext() {
        IN_CASC_CONTEXT.set(Boolean.TRUE);
    }

    /**
     * Called by ConfigurationAsCode to indicate that JCasC configuration has completed.
     */
    public static void exitCascContext() {
        IN_CASC_CONTEXT.set(Boolean.FALSE);
    }

    /**
     * Check if we're currently in a JCasC configuration context.
     * @return true if within JCasC configuration
     */
    public static boolean isInCascContext() {
        return IN_CASC_CONTEXT.get();
    }

    @Override
    public void onCreated(@NonNull Item item) {
        // Only process items created during JCasC configuration
        if (!isInCascContext()) {
            return;
        }

        Class<?> itemClass = item.getClass();

        // Check if this is a MultiBranchProject or OrganizationFolder using reflection
        // to avoid hard dependency on branch-api plugin
        if (isInstanceOf(itemClass, MULTI_BRANCH_PROJECT_CLASS)) {
            fixBranchProject(item, "MultiBranchProject");
        } else if (isInstanceOf(itemClass, ORGANIZATION_FOLDER_CLASS)) {
            fixBranchProject(item, "OrganizationFolder");
        }
    }

    /**
     * Check if a class is an instance of a class specified by name.
     * This allows checking without a direct class dependency.
     *
     * @param clazz the class to check
     * @param className the fully qualified class name to check against
     * @return true if clazz is assignable to the class specified by className
     */
    private boolean isInstanceOf(Class<?> clazz, String className) {
        try {
            Class<?> targetClass = Class.forName(className);
            return targetClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            // Class not available (branch-api plugin not installed)
            return false;
        }
    }

    /**
     * Fix credential initialization for MultiBranchProject or OrganizationFolder.
     * This is equivalent to clicking "configure" and "save" in the UI.
     *
     * @param item the item to fix
     * @param itemType the type name for logging
     */
    private void fixBranchProject(Item item, String itemType) {
        String itemName = item.getFullName();
        LOGGER.log(Level.FINE, "Fixing credential binding for {0}: {1}", new Object[] {itemType, itemName});

        try {
            // Trigger recalculation which initializes credentials
            triggerRecalculation(item);

            // Save the item to persist the initialized state
            // All Items implement Saveable, so cast directly
            item.save();

            LOGGER.log(Level.FINE, "Successfully fixed {0}: {1}", new Object[] {itemType, itemName});
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save " + itemType + " after credential fix: " + itemName, e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fix credential binding for " + itemType + ": " + itemName, e);
        }
    }

    /**
     * Trigger recalculation for the branch project.
     * This mimics what happens when the form is submitted in the UI.
     *
     * @param item the item to recalculate
     */
    private void triggerRecalculation(Item item) {
        // Try multiple approaches to trigger the recalculation

        // Approach 1: Try to call recalculateAfterSubmitted if available
        try {
            Method method = findMethod(item.getClass(), "recalculateAfterSubmitted");
            if (method != null) {
                method.setAccessible(true);
                method.invoke(item);
                LOGGER.log(Level.FINE, "Called recalculateAfterSubmitted on {0}", item.getFullName());
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not call recalculateAfterSubmitted", e);
        }

        // Approach 2: Try to get SCM sources (triggers lazy initialization)
        try {
            Method getSCMSources = findMethod(item.getClass(), "getSCMSources");
            if (getSCMSources != null) {
                getSCMSources.setAccessible(true);
                getSCMSources.invoke(item);
                LOGGER.log(Level.FINE, "Called getSCMSources on {0}", item.getFullName());
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not call getSCMSources", e);
        }

        // Approach 3: Try to get navigators (for OrganizationFolder)
        try {
            Method getNavigators = findMethod(item.getClass(), "getNavigators");
            if (getNavigators != null) {
                getNavigators.setAccessible(true);
                getNavigators.invoke(item);
                LOGGER.log(Level.FINE, "Called getNavigators on {0}", item.getFullName());
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not call getNavigators", e);
        }

        // Approach 4: Try calling afterSave which some implementations use
        try {
            Method afterSave = findMethod(item.getClass(), "afterSave");
            if (afterSave != null) {
                afterSave.setAccessible(true);
                afterSave.invoke(item);
                LOGGER.log(Level.FINE, "Called afterSave on {0}", item.getFullName());
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not call afterSave", e);
        }
    }

    /**
     * Find a method in the class hierarchy.
     *
     * @param clazz the class to search
     * @param methodName the method name
     * @return the method if found, null otherwise
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
