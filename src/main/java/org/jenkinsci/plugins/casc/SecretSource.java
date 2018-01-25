package org.jenkinsci.plugins.casc;

import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

public abstract class SecretSource implements ExtensionPoint {
    /**
     *
     * @param secret
     * @return the revealed secret. Null in the case that the implementation is not replacing anything. Throws exception
     * if the secret could not be fetched.
     */
    public abstract String reveal(String secret);

    public static List<SecretSource> all() {
        List<SecretSource> all = new ArrayList<>();
        all.addAll(Jenkins.getInstance().getExtensionList(SecretSource.class));
        return all;
    }

}
